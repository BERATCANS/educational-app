package com.beratcan.app.service;

import com.beratcan.app.dto.request.PurchaseRequest;
import com.beratcan.app.dto.response.PurchaseItemResponse;
import com.beratcan.app.dto.response.PurchaseResponse;
import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.Cart;
import com.beratcan.app.entity.CartItem;
import com.beratcan.app.entity.Purchase;
import com.beratcan.app.entity.PurchaseItem;
import com.beratcan.app.entity.StudentProfile;
import com.beratcan.app.entity.User;
import com.beratcan.app.entity.enums.Role;
import com.beratcan.app.entity.enums.PurchaseStatus;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.CartNotFoundException;
import com.beratcan.app.exception.ExternalServiceException;
import com.beratcan.app.exception.IdempotencyConflictException;
import com.beratcan.app.exception.PriceChangedException;
import com.beratcan.app.exception.PurchaseNotFoundException;
import com.beratcan.app.exception.UnauthorizedOperationException;
import com.beratcan.app.exception.UserNotFoundException;
import com.beratcan.app.repository.BookRepository;
import com.beratcan.app.repository.CartRepository;
import com.beratcan.app.repository.PurchaseRepository;
import com.beratcan.app.repository.StudentProfileRepository;
import com.beratcan.app.repository.UserRepository;
import com.beratcan.app.constants.AppTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PurchaseService {

    private static final Pattern IDEMPOTENCY_KEY_PATTERN =
            Pattern.compile("^[A-Za-z0-9._~+-]{1,80}$");

    private final PurchaseRepository purchaseRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final CartRepository cartRepository;
    private final DiscountCodeService discountCodeService;


    @Transactional(readOnly = true)
    public List<PurchaseResponse> getUserPurchases(Long userId) {

        List<Purchase> purchases = purchaseRepository.findByUserIdOrderByPurchaseDateDesc(userId);

        log.info("Purchases retrieved: userId={}, purchaseCount={}", userId, purchases.size());

        return purchases.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PurchaseResponse> getUserCompletedPurchases(Long userId) {

        List<Purchase> purchases = purchaseRepository
                .findByUserIdAndStatusOrderByPurchaseDateDesc(userId, PurchaseStatus.COMPLETED);

        log.info("Completed purchases retrieved: userId={}, completedCount={}", userId, purchases.size());

        return purchases.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PurchaseResponse getPurchaseById(Long purchaseId, Long userId) {
        Purchase purchase = purchaseRepository.findByIdWithUserAndBook(purchaseId)
                .orElseThrow(() -> {
                    PurchaseNotFoundException ex = new PurchaseNotFoundException(purchaseId);
                    log.error("Purchase not found: purchaseId={}, userId={}", purchaseId, userId, ex);
                    return ex;
                });

        if (!purchase.getStudentProfile().getUser().getId().equals(userId)) {
            log.warn("Unauthorized purchase access attempt: purchaseId={}, requestingUserId={}, ownerUserId={}",
                    purchaseId, userId, purchase.getStudentProfile().getUser().getId());
            throw new UnauthorizedOperationException("Bu satın alma kaydına erişim yetkiniz yok");
        }

        return mapToResponse(purchase);
    }

    @Transactional
    public PurchaseResponse checkoutCart(Long userId) {
        return checkoutCart(userId, null);
    }

    @Transactional(readOnly = true)
    public Optional<PurchaseResponse> findByIdempotencyKey(Long userId, String idempotencyKey) {
        return purchaseRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .map(this::mapToResponse);
    }

    @Transactional
    public PurchaseResponse checkoutCart(Long userId, String idempotencyKey) {
        long startTime = System.currentTimeMillis();

        log.info("EVENT: Cart checkout started | userId={} | idempotencyKey={}",
                userId, idempotencyKey);

        if (idempotencyKey != null) {
            validateIdempotencyKey(idempotencyKey);

            Optional<Purchase> existing =
                    purchaseRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
            if (existing.isPresent()) {
                log.info("EVENT: Idempotent checkout replay | userId={} | key={} | purchaseId={}",
                        userId, idempotencyKey, existing.get().getId());
                return mapToResponse(existing.get());
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    UserNotFoundException ex = new UserNotFoundException(userId);
                    log.error("User not found for cart checkout: userId={}", userId, ex);
                    return ex;
                });

        validateBookOwnershipAllowed(user);

        // Email doğrulama kontrolü
        if (user.getEmailVerified() == null || !user.getEmailVerified()) {
            log.warn("Checkout attempted with unverified email: userId={}", userId);
            throw new BadRequestException("Satın alma işlemi yapabilmek için lütfen email adresinizi doğrulayın.");
        }

        // Sepeti getir (pessimistic lock ile)
        Cart cart = cartRepository.findByUserIdWithLockAndItems(userId)
                .orElseThrow(() -> {
                    CartNotFoundException ex = new CartNotFoundException(userId);
                    log.error("Cart not found for user: userId={}", userId, ex);
                    return ex;
                });

        // Lock altında ikinci idempotency pre-check.
        // Pre-check (line 124) ile lock alımı arasında paralel bir checkout
        // commit etmiş olabilir; o durumda yeni Purchase yaratmadan cached
        // response döndürmeliyiz, yoksa Cart @Version optimistic lock fail
        // edip 500 oluyor.
        if (idempotencyKey != null) {
            Optional<Purchase> existing =
                    purchaseRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
            if (existing.isPresent()) {
                log.info("EVENT: Idempotent checkout replay (post-lock) | userId={} | key={} | purchaseId={}",
                        userId, idempotencyKey, existing.get().getId());
                return mapToResponse(existing.get());
            }
        }

        int cartItemCount = cart.getItems().size();
        BigDecimal initialTotalAmount = cart.getItems().stream()
                .map(item -> item.getPriceAtAdd() != null ? item.getPriceAtAdd() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("EVENT: Cart checkout details | userId={} | itemCount={} | initialTotalAmount={}",
                userId, cartItemCount, initialTotalAmount);

        if (cart.getItems().isEmpty()) {
            log.warn("Attempted to checkout empty cart: userId={}", userId);
            throw new BadRequestException("Sepetiniz boş");
        }

        // ADIM 1: Tüm kitapları validate et
        List<Book> validBooks = new ArrayList<>();
        List<BigDecimal> validPrices = new ArrayList<>();
        List<String> validationErrors = new ArrayList<>();
        List<PriceChangedException.ChangedItem> priceChanges = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Book bookFromCart = cartItem.getBook();

            if (bookFromCart == null) {
                validationErrors.add("Sepetteki bir kitap artık mevcut değil (silinmiş)");
                log.warn("Book not found in cart item: cartItemId={}, userId={}", cartItem.getId(), userId);
                continue;
            }

            final Book book;
            try {
                Book verifiedBook = bookRepository.findById(bookFromCart.getId())
                        .orElse(null);
                if (verifiedBook == null) {
                    String bookTitle = bookFromCart.getTitle() != null ? bookFromCart.getTitle() : "Bilinmeyen kitap";
                    validationErrors.add(bookTitle + ": Kitap artık mevcut değil (silinmiş)");
                    continue;
                }
                book = verifiedBook;
            } catch (Exception e) {
                log.error("Error verifying book existence: bookId={}, userId={}", bookFromCart.getId(), userId, e);
                String bookTitle = bookFromCart.getTitle() != null ? bookFromCart.getTitle() : "Bilinmeyen kitap";
                validationErrors.add(bookTitle + ": Kitap kontrol edilemedi");
                continue;
            }

            BigDecimal priceAtAdd = cartItem.getPriceAtAdd();
            BigDecimal currentPrice = book.getPrice();

            if (priceAtAdd != null && currentPrice != null &&
                    priceAtAdd.compareTo(currentPrice) != 0) {
                priceChanges.add(new PriceChangedException.ChangedItem(
                        cartItem.getId(), book.getId(), book.getTitle(), priceAtAdd, currentPrice));
                continue;
            }

            if (!book.isPaid()) {
                validationErrors.add(book.getTitle() + ": Ücretsiz kitap");
                continue;
            }

            if (priceAtAdd == null || priceAtAdd.compareTo(BigDecimal.ZERO) <= 0) {
                validationErrors.add(book.getTitle() + ": Geçersiz fiyat");
                continue;
            }

            final Long bookId = book.getId();
            boolean alreadyOwned = user.getOwnedBooks().stream()
                    .anyMatch(b -> b.getId().equals(bookId));
            if (alreadyOwned) {
                validationErrors.add(book.getTitle() + ": Zaten kütüphanenizde mevcut");
                continue;
            }

            validBooks.add(book);
            validPrices.add(priceAtAdd);
            totalAmount = totalAmount.add(priceAtAdd);
        }

        // Fiyat değişimi varsa checkout'u 409 ile durdur — kullanıcı yeni toplamı
        // /api/cart/refresh-prices ile onaylayıp tekrar denemeli.
        if (!priceChanges.isEmpty()) {
            log.info("EVENT: Checkout blocked by price change | userId={} | changedCount={}",
                    userId, priceChanges.size());
            throw new PriceChangedException(priceChanges);
        }

        log.info("Cart validation completed: userId={}, totalItems={}, validBooks={}, invalidBooks={}, totalAmount={}",
                userId, cartItemCount, validBooks.size(), validationErrors.size(), totalAmount);

        if (validBooks.isEmpty()) {
            log.error("No valid books in cart: userId={}, errors={}", userId, validationErrors);
            throw new BadRequestException("Sepette satın alınabilir kitap yok: " +
                    String.join(", ", validationErrors));
        }

        // ADIM 2: İndirim kontrolü
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal finalTotalAmount = totalAmount;
        String discountCodeWarning = null;

        if (cart.getAppliedDiscountCode() != null) {
            com.beratcan.app.dto.response.DiscountValidationResult validation = discountCodeService
                    .validateDiscountCode(cart.getAppliedDiscountCode().getCode(), userId, cart);

            if (validation.isValid()) {
                discountAmount = validation.getDiscountAmount();
                finalTotalAmount = totalAmount.subtract(discountAmount);
                log.info("EVENT: Discount applied | userId={} | code={} | discountAmount={} | finalTotal={}",
                        userId, cart.getAppliedDiscountCode().getCode(), discountAmount, finalTotalAmount);
            } else {
                log.warn("Discount code validation failed during checkout: userId={}, code={}, error={}",
                        userId, cart.getAppliedDiscountCode().getCode(), validation.getErrorMessage());
                discountCodeWarning = "İndirim kodu geçersiz: " + validation.getErrorMessage() +
                        " Ödeme indirimsiz olarak devam edecek.";
                cart.setAppliedDiscountCode(null);
                cart.setDiscountAmount(null);
                cart.setTotalPriceAfterDiscount(null);
            }
        }

        // ADIM 3: Purchase kaydı oluştur
        String transactionId = generateTransactionId();
        LocalDateTime purchaseDate = AppTime.now();

        StudentProfile studentProfile = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Öğrenci profili bulunamadı. userId=" + userId));

        Purchase purchase = new Purchase();
        purchase.setStudentProfile(studentProfile);
        purchase.setTotalPrice(totalAmount);
        purchase.setTotalDiscount(discountAmount);
        purchase.setFinalPrice(finalTotalAmount);
        purchase.setItemCount(validBooks.size());
        purchase.setPurchaseDate(purchaseDate);
        purchase.setPaymentMethod("Sepet ile satın alma");
        purchase.setTransactionId(transactionId);
        purchase.setStatus(PurchaseStatus.PENDING);
        purchase.setIdempotencyKey(idempotencyKey);

        if (cart.getAppliedDiscountCode() != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            purchase.setDiscountCode(cart.getAppliedDiscountCode());
        }

        // ADIM 4: PurchaseItem'lar oluştur
        BigDecimal totalDiscountDistributed = BigDecimal.ZERO;

        for (int bookIndex = 0; bookIndex < validBooks.size(); bookIndex++) {
            Book book = validBooks.get(bookIndex);
            BigDecimal itemPrice = validPrices.get(bookIndex);

            BigDecimal bookDiscount = BigDecimal.ZERO;
            boolean isLastBook = (bookIndex == validBooks.size() - 1);

            if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                if (isLastBook) {
                    bookDiscount = discountAmount.subtract(totalDiscountDistributed);
                } else {
                    bookDiscount = itemPrice
                            .divide(totalAmount, 10, RoundingMode.HALF_UP)
                            .multiply(discountAmount)
                            .setScale(2, RoundingMode.HALF_UP);
                    totalDiscountDistributed = totalDiscountDistributed.add(bookDiscount);
                }
            }

            BigDecimal bookFinalPrice = itemPrice.subtract(bookDiscount);
            if (bookFinalPrice.compareTo(BigDecimal.ZERO) < 0) {
                bookFinalPrice = BigDecimal.ZERO;
            }

            PurchaseItem item = new PurchaseItem();
            item.setBook(book);
            item.setPrice(itemPrice);
            item.setDiscountAmount(bookDiscount);
            item.setFinalPrice(bookFinalPrice);

            purchase.addItem(item);
        }

        Purchase savedPurchase;
        try {
            savedPurchase = purchaseRepository.saveAndFlush(purchase);
        } catch (DataIntegrityViolationException e) {
            if (idempotencyKey != null && e.getMessage() != null
                    && e.getMessage().contains("uk_purchases_student_idempotency")) {
                log.warn("Idempotency conflict on insert: userId={}, key={}",
                        userId, idempotencyKey);
                throw new IdempotencyConflictException(userId, idempotencyKey);
            }
            throw e;
        } catch (ObjectOptimisticLockingFailureException e) {
            // Defansif: Cart/CartItem @Version ihlali. Lock altındaki ikinci
            // pre-check normalde bunu engellemeli; yine de bir refactor lock'u
            // bypass ederse veya başka bir paralel yazıcı oluşursa, idempotency
            // key varlığında controller cached response döndürebilsin diye
            // IdempotencyConflictException'a çeviriyoruz.
            if (idempotencyKey != null) {
                log.warn("Optimistic lock during checkout, treating as idempotency replay: userId={}, key={}",
                        userId, idempotencyKey);
                throw new IdempotencyConflictException(userId, idempotencyKey);
            }
            throw e;
        }

        // ADIM 5: Ödeme işlemi
        PurchaseRequest paymentRequest = new PurchaseRequest();
        paymentRequest.setPaymentMethod("Sepet ile satın alma");

        Purchase dummyPurchase = new Purchase();
        dummyPurchase.setTotalPrice(finalTotalAmount);
        dummyPurchase.setTransactionId(transactionId);

        log.info("Processing payment: userId={}, finalTotalAmount={}, transactionId={}",
                userId, finalTotalAmount, transactionId);

        boolean paymentSuccess = processPayment(dummyPurchase, paymentRequest);

        // ADIM 6: Ödeme sonucu
        if (paymentSuccess) {
            log.info("Payment successful: userId={}, finalTotalAmount={}, transactionId={}",
                    userId, finalTotalAmount, transactionId);

            savedPurchase.setStatus(PurchaseStatus.COMPLETED);

            for (Book book : validBooks) {
                try {
                    user.getOwnedBooks().add(book);
                } catch (DataIntegrityViolationException e) {
                    if (e.getMessage() != null && e.getMessage().contains("uk_user_book")) {
                        log.warn("Book already in library (checkout): userId={}, bookId={}", userId, book.getId());
                    } else {
                        throw e;
                    }
                }
            }

            purchaseRepository.save(savedPurchase);
            userRepository.save(user);

            if (cart.getAppliedDiscountCode() != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                discountCodeService.useDiscountCode(
                        cart.getAppliedDiscountCode().getId(),
                        userId,
                        transactionId);
                log.info("Discount code used after successful payment: code={}, transactionId={}",
                        cart.getAppliedDiscountCode().getCode(), transactionId);
            }
        } else {
            log.error("Payment failed: userId={}, finalTotalAmount={}, transactionId={}",
                    userId, finalTotalAmount, transactionId);

            savedPurchase.setStatus(PurchaseStatus.FAILED);
            purchaseRepository.save(savedPurchase);

            throw new ExternalServiceException("Ödeme işlemi başarısız oldu. Toplam tutar: " +
                    finalTotalAmount + " TL. Lütfen tekrar deneyin.");
        }

        // Sepeti temizle
        cart.getItems().clear();
        cart.setAppliedDiscountCode(null);
        cart.setDiscountAmount(null);
        cart.setTotalPriceAfterDiscount(null);
        cartRepository.save(cart);
        log.info("Cart cleared after successful checkout: userId={}", userId);

        if (!validationErrors.isEmpty()) {
            log.warn("Some books were excluded from cart checkout: userId={}, excludedCount={}, reasons={}",
                    userId, validationErrors.size(), String.join(", ", validationErrors));
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info(
                "EVENT: Cart checkout completed | userId={} | itemCount={} | totalAmount={} | transactionId={} | duration={}ms",
                userId, savedPurchase.getItemCount(), finalTotalAmount, transactionId, duration);

        if (duration > 5000) {
            log.warn("PERF: Cart checkout slow | duration={}ms | threshold=5000ms | userId={} | transactionId={}",
                    duration, userId, transactionId);
        }

        PurchaseResponse response = mapToResponse(savedPurchase);
        if (discountCodeWarning != null) {
            response.setInfoMessage(discountCodeWarning);
        }
        return response;
    }

    private boolean processPayment(Purchase purchase, PurchaseRequest request) {
        // TODO: Gerçek ödeme gateway'i (Stripe, iyzico, vb.) ile entegrasyon
        boolean success = true;

        if (!success) {
            log.error("Payment processing failed: transactionId={}", purchase.getTransactionId());
        }

        return success;
    }

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString();
    }

    private PurchaseResponse mapToResponse(Purchase purchase) {
        PurchaseResponse response = new PurchaseResponse();
        response.setId(purchase.getId());
        User user = purchase.getStudentProfile().getUser();
        response.setUserId(user.getId());
        response.setUserName(user.getFirstName() + " " + user.getLastName());
        response.setTotalPrice(purchase.getTotalPrice());
        response.setTotalDiscount(purchase.getTotalDiscount());
        response.setFinalPrice(purchase.getFinalPrice());
        response.setItemCount(purchase.getItemCount());
        response.setPurchaseDate(purchase.getPurchaseDate());
        response.setStatus(purchase.getStatus());
        response.setTransactionId(purchase.getTransactionId());
        response.setPaymentMethod(purchase.getPaymentMethod());

        if (purchase.getDiscountCode() != null) {
            response.setDiscountCode(purchase.getDiscountCode().getCode());
        }

        if (purchase.getItems() != null) {
            response.setItems(purchase.getItems().stream()
                    .map(this::mapToItemResponse)
                    .collect(Collectors.toList()));
        }

        return response;
    }

    private PurchaseItemResponse mapToItemResponse(PurchaseItem item) {
        PurchaseItemResponse response = new PurchaseItemResponse();
        response.setId(item.getId());
        response.setBookId(item.getBook().getId());
        response.setBookTitle(item.getBook().getTitle());
        response.setBookCoverImageUrl(item.getBook().getCoverImageUrl());
        response.setBookPublisher(item.getBook().getPublisher() != null
                ? item.getBook().getPublisher().getName()
                : null);
        response.setPrice(item.getPrice());
        response.setDiscountAmount(item.getDiscountAmount());
        response.setFinalPrice(item.getFinalPrice());
        return response;
    }

    private void validateBookOwnershipAllowed(User user) {
        if (user.getRole() == Role.PARENT) {
            throw new BadRequestException("Parent hesapları kitap satın alamaz.");
        }
    }

    private void validateIdempotencyKey(String key) {
        if (!IDEMPOTENCY_KEY_PATTERN.matcher(key).matches()) {
            throw new BadRequestException(
                    "Idempotency-Key must match [A-Za-z0-9._~+-]{1,80}");
        }
    }
}
