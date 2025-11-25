package com.beratcan.app.service;

import com.beratcan.app.dto.request.PurchaseRequest;
import com.beratcan.app.dto.response.PurchaseResponse;
import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.Cart;
import com.beratcan.app.entity.CartItem;
import com.beratcan.app.entity.Purchase;
import com.beratcan.app.entity.User;
import com.beratcan.app.entity.enums.PurchaseStatus;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.BookNotFoundException;
import com.beratcan.app.exception.CartNotFoundException;
import com.beratcan.app.exception.ExternalServiceException;
import com.beratcan.app.exception.PurchaseNotFoundException;
import com.beratcan.app.exception.UnauthorizedOperationException;
import com.beratcan.app.exception.UserNotFoundException;
import com.beratcan.app.repository.BookRepository;
import com.beratcan.app.repository.CartRepository;
import com.beratcan.app.repository.PurchaseRepository;
import com.beratcan.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final DiscountCodeService discountCodeService;

    @Transactional
    public PurchaseResponse buyBook(Long userId, Long bookId, PurchaseRequest request) {
        log.info("Starting book purchase: userId={}, bookId={}, paymentMethod={}",
                userId, bookId, request.getPaymentMethod());

        // Kullanıcıyı getir
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    UserNotFoundException ex = new UserNotFoundException(userId);
                    log.error("User not found for purchase: userId={}", userId, ex);
                    return ex;
                });

        // Kitabı getir
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> {
                    BookNotFoundException ex = new BookNotFoundException(bookId);
                    log.error("Book not found for purchase: bookId={}, userId={}", bookId, userId, ex);
                    return ex;
                });

        // Kitap ücretsiz mi kontrol et
        if (!book.isPaid()) {
            log.warn("Attempted to purchase free book: userId={}, bookId={}, title={}",
                    userId, bookId, book.getTitle());
            throw new BadRequestException("Bu kitap zaten ücretsizdir");
        }

        // Kitabın fiyatı var mı kontrol et
        if (book.getPrice() == null || book.getPrice().signum() <= 0) {
            log.error("Invalid book price: bookId={}, price={}", bookId, book.getPrice());
            throw new BadRequestException("Kitap fiyatı geçerli değil");
        }

        // Kullanıcı bu kitabı daha önce satın almış mı kontrol et
        boolean alreadyOwned = user.getOwnedBooks().stream()
                .anyMatch(b -> b.getId().equals(bookId));

        if (alreadyOwned) {
            log.info("Book already owned by user: userId={}, bookId={}, title={}",
                    userId, bookId, book.getTitle());

            // ✅ Exception değil, bilgi mesajı olarak dön
            // Önceki satın alma kaydını bul
            Purchase existingPurchase = purchaseRepository
                    .findByUserIdAndBookIdAndStatus(userId, bookId, PurchaseStatus.COMPLETED)
                    .orElse(null);

            PurchaseResponse response;
            if (existingPurchase != null) {
                response = mapToResponse(existingPurchase);
            } else {
                // Kayıt yoksa yeni bir response oluştur
                response = new PurchaseResponse();
                response.setUserId(userId);
                response.setUserName(user.getFirstName() + " " + user.getLastName());
                response.setBookId(bookId);
                response.setBookTitle(book.getTitle());
                response.setBookCoverImageUrl(book.getCoverImageUrl());
                response.setBookPublisher(book.getPublisher() != null ? book.getPublisher().getName() : null);
                response.setPrice(book.getPrice());
                response.setStatus(PurchaseStatus.COMPLETED);
            }
            response.setInfoMessage(
                    "'" + book.getTitle() + "' zaten kütüphanenizde mevcut. Tekrar satın almanıza gerek yok.");
            return response;
        }

        // Tamamlanmış bir satın alma kaydı var mı kontrol et (gereksiz oldu ama
        // bırakalım)
        purchaseRepository.findByUserIdAndBookIdAndStatus(userId, bookId, PurchaseStatus.COMPLETED)
                .ifPresent(p -> {
                    // Bu duruma artık gelmeyecek çünkü yukarıda yakaladık
                });

        // Purchase kaydı oluştur
        String transactionId = generateTransactionId();

        Purchase purchase = new Purchase();
        purchase.setUser(user);
        purchase.setBook(book);
        purchase.setPrice(book.getPrice());
        purchase.setPurchaseDate(LocalDateTime.now());
        purchase.setPaymentMethod(request.getPaymentMethod());
        purchase.setTransactionId(transactionId);

        log.info("Processing payment: userId={}, bookId={}, amount={}, transactionId={}",
                userId, bookId, book.getPrice(), transactionId);

        // Burada gerçek ödeme işlemi yapılacak
        // Şimdilik her satın alma işlemini başarılı kabul ediyoruz
        boolean paymentSuccess = processPayment(purchase, request);

        if (paymentSuccess) {
            purchase.setStatus(PurchaseStatus.COMPLETED);
            log.info("Payment successful: userId={}, bookId={}, transactionId={}",
                    userId, bookId, transactionId);

            // Kitabı kullanıcının kütüphanesine ekle
            try {
                user.getOwnedBooks().add(book);
                userRepository.save(user);
                log.debug("Book added to user library: userId={}, bookId={}", userId, bookId);
            } catch (DataIntegrityViolationException e) {
                // DB constraint hatası (duplicate entry) yakalandı
                if (e.getMessage() != null && e.getMessage().contains("uk_user_book")) {
                    log.warn("Duplicate book ownership attempt prevented by DB constraint: userId={}, bookId={}",
                            userId, bookId);
                    // Kitap zaten kütüphanede, ama purchase kaydı oluşturuldu - bu normal bir durum
                    // (kullanıcı daha önce eklemiş ama purchase kaydı yok olabilir)
                } else {
                    throw e; // Diğer constraint hataları için
                }
            }
        } else {
            purchase.setStatus(PurchaseStatus.FAILED);
            log.error("Payment failed: userId={}, bookId={}, transactionId={}",
                    userId, bookId, transactionId);
        }

        // Purchase kaydını kaydet
        Purchase savedPurchase = purchaseRepository.save(purchase);
        log.info("Purchase completed: purchaseId={}, userId={}, bookId={}, status={}, transactionId={}",
                savedPurchase.getId(), userId, bookId, savedPurchase.getStatus(), transactionId);

        return mapToResponse(savedPurchase);
    }

    public List<PurchaseResponse> getUserPurchases(Long userId) {
        log.debug("Fetching all purchases for user: userId={}", userId);

        List<Purchase> purchases = purchaseRepository.findByUserIdOrderByPurchaseDateDesc(userId);

        log.info("Purchases retrieved: userId={}, purchaseCount={}", userId, purchases.size());

        return purchases.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<PurchaseResponse> getUserCompletedPurchases(Long userId) {
        log.debug("Fetching completed purchases for user: userId={}", userId);

        List<Purchase> purchases = purchaseRepository
                .findByUserIdAndStatusOrderByPurchaseDateDesc(userId, PurchaseStatus.COMPLETED);

        log.info("Completed purchases retrieved: userId={}, completedCount={}", userId, purchases.size());

        return purchases.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public PurchaseResponse getPurchaseById(Long purchaseId, Long userId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> {
                    PurchaseNotFoundException ex = new PurchaseNotFoundException(purchaseId);
                    log.error("Purchase not found: purchaseId={}, userId={}", purchaseId, userId, ex);
                    return ex;
                });

        // Kullanıcının kendi satın alma kaydını görmesini sağla
        if (!purchase.getUser().getId().equals(userId)) {
            log.warn("Unauthorized purchase access attempt: purchaseId={}, requestingUserId={}, ownerUserId={}",
                    purchaseId, userId, purchase.getUser().getId());
            throw new UnauthorizedOperationException("Bu satın alma kaydına erişim yetkiniz yok");
        }

        return mapToResponse(purchase);
    }

    /**
     * Sepetteki tüm kitapları toplu olarak satın al
     * TEK ÖDEME işlemi ile tüm sepeti satın alır
     */
    @Transactional
    public List<PurchaseResponse> checkoutCart(Long userId) {
        log.info("Starting cart checkout: userId={}", userId);

        // Kullanıcıyı getir
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    UserNotFoundException ex = new UserNotFoundException(userId);
                    log.error("User not found for cart checkout: userId={}", userId, ex);
                    return ex;
                });

        // Sepeti getir
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    CartNotFoundException ex = new CartNotFoundException(userId);
                    log.error("Cart not found for user: userId={}", userId, ex);
                    return ex;
                });

        int cartItemCount = cart.getItems().size();

        // Sepet boş mu kontrol et
        if (cart.getItems().isEmpty()) {
            log.warn("Attempted to checkout empty cart: userId={}", userId);
            throw new BadRequestException("Sepetiniz boş");
        }

        // ADIM 1: Tüm kitapları validate et ve satın alınabilir olanları listele

        List<Book> validBooks = new ArrayList<>();
        List<String> validationErrors = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Book book = cartItem.getBook();

            // Validasyonlar
            if (!book.isPaid()) {
                validationErrors.add(book.getTitle() + ": Ücretsiz kitap");
                continue;
            }

            if (book.getPrice() == null || book.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                validationErrors.add(book.getTitle() + ": Geçersiz fiyat");
                continue;
            }

            // Zaten sahip mi kontrol et
            boolean alreadyOwned = user.getOwnedBooks().stream()
                    .anyMatch(b -> b.getId().equals(book.getId()));
            if (alreadyOwned) {
                validationErrors.add(book.getTitle() + ": Zaten kütüphanenizde mevcut");
                continue;
            }

            // Geçerli kitap, listeye ekle
            validBooks.add(book);
            totalAmount = totalAmount.add(book.getPrice());
        }

        log.info("Cart validation completed: userId={}, totalItems={}, validBooks={}, invalidBooks={}, totalAmount={}",
                userId, cartItemCount, validBooks.size(), validationErrors.size(), totalAmount);

        // Hiç geçerli kitap yoksa hata fırlat
        if (validBooks.isEmpty()) {
            log.error("No valid books in cart: userId={}, errors={}", userId, validationErrors);
            throw new BadRequestException("Sepette satın alınabilir kitap yok: " +
                    String.join(", ", validationErrors));
        }

        // ADIM 2: İndirim kontrolü ve uygulama
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal finalTotalAmount = totalAmount;

        if (cart.getAppliedDiscountCode() != null) {
            // İndirim kodunu tekrar validate et (son kontrol)
            com.beratcan.app.dto.response.DiscountValidationResult validation = discountCodeService
                    .validateDiscountCode(cart.getAppliedDiscountCode().getCode(), userId, cart);

            if (validation.isValid()) {
                discountAmount = validation.getDiscountAmount();
                finalTotalAmount = totalAmount.subtract(discountAmount);
                log.info("Discount applied: userId={}, code={}, discountAmount={}, finalTotal={}",
                        userId, cart.getAppliedDiscountCode().getCode(), discountAmount, finalTotalAmount);
            } else {
                log.warn("Discount code validation failed during checkout: userId={}, code={}, error={}",
                        userId, cart.getAppliedDiscountCode().getCode(), validation.getErrorMessage());
                // İndirim geçersiz, devam et ama indirimsiz
                cart.setAppliedDiscountCode(null);
                cart.setDiscountAmount(null);
                cart.setTotalPriceAfterDiscount(null);
            }
        }

        // ADIM 3: TEK SEFERDE tüm sepet için ödeme işlemini yap
        String transactionId = generateTransactionId();
        log.info(
                "Processing bulk payment: userId={}, bookCount={}, totalAmount={}, discountAmount={}, finalTotal={}, transactionId={}",
                userId, validBooks.size(), totalAmount, discountAmount, finalTotalAmount, transactionId);

        PurchaseRequest paymentRequest = new PurchaseRequest();
        paymentRequest.setPaymentMethod("Sepet ile satın alma");

        // Dummy purchase objesi oluştur (sadece processPayment için)
        Purchase dummyPurchase = new Purchase();
        dummyPurchase.setPrice(finalTotalAmount); // İndirim sonrası toplam tutar
        dummyPurchase.setTransactionId(transactionId);

        boolean paymentSuccess = processPayment(dummyPurchase, paymentRequest);

        // ADIM 4: Ödeme başarısız ise hiçbir şey yapma
        if (!paymentSuccess) {
            log.error("Bulk payment failed: userId={}, finalTotalAmount={}, transactionId={}",
                    userId, finalTotalAmount, transactionId);
            throw new ExternalServiceException("Ödeme işlemi başarısız oldu. Toplam tutar: " +
                    finalTotalAmount + " TL. Lütfen tekrar deneyin.");
        }

        log.info("Bulk payment successful: userId={}, finalTotalAmount={}, discountAmount={}, transactionId={}",
                userId, finalTotalAmount, discountAmount, transactionId);

        // ADIM 5: İndirim kodunu kullan (kaydet)
        if (cart.getAppliedDiscountCode() != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            discountCodeService.useDiscountCode(
                    cart.getAppliedDiscountCode().getId(),
                    userId,
                    transactionId);
        }

        // ADIM 6: Ödeme başarılı - Tüm kitaplar için Purchase kayıtları oluştur
        List<PurchaseResponse> successfulPurchases = new ArrayList<>();
        LocalDateTime purchaseDate = LocalDateTime.now();

        // Her kitap için indirim tutarını hesapla (proportional)
        BigDecimal discountPerBook = BigDecimal.ZERO;
        if (discountAmount.compareTo(BigDecimal.ZERO) > 0 && validBooks.size() > 0) {
            discountPerBook = discountAmount.divide(
                    new BigDecimal(validBooks.size()), 2, RoundingMode.HALF_UP);
        }

        for (Book book : validBooks) {
            log.debug("Creating purchase record: userId={}, bookId={}, transactionId={}",
                    userId, book.getId(), transactionId);

            BigDecimal bookDiscount = discountPerBook;
            BigDecimal bookFinalPrice = book.getPrice().subtract(bookDiscount);

            Purchase purchase = new Purchase();
            purchase.setUser(user);
            purchase.setBook(book);
            purchase.setPrice(book.getPrice()); // İndirim öncesi fiyat
            purchase.setPurchaseDate(purchaseDate);
            purchase.setPaymentMethod("Sepet ile satın alma");
            purchase.setTransactionId(transactionId); // Aynı transaction ID
            purchase.setStatus(PurchaseStatus.COMPLETED);

            // İndirim bilgileri
            if (cart.getAppliedDiscountCode() != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                purchase.setDiscountCode(cart.getAppliedDiscountCode());
                purchase.setDiscountAmount(bookDiscount);
                purchase.setFinalPrice(bookFinalPrice);
            } else {
                purchase.setFinalPrice(book.getPrice()); // İndirim yoksa normal fiyat
            }

            // Kitabı kullanıcının kütüphanesine ekle
            try {
                user.getOwnedBooks().add(book);
            } catch (DataIntegrityViolationException e) {
                // DB constraint hatası (duplicate entry) yakalandı
                // Bu durumda sadece purchase kaydını oluştur, kütüphaneye ekleme
                if (e.getMessage() != null && e.getMessage().contains("uk_user_book")) {
                    log.warn("Book already in library (checkout): userId={}, bookId={}", userId, book.getId());
                    // Kitap zaten kütüphanede, purchase kaydı oluşturmaya devam et
                } else {
                    throw e; // Diğer constraint hataları için
                }
            }

            // Purchase'ı kaydet
            Purchase savedPurchase = purchaseRepository.save(purchase);
            successfulPurchases.add(mapToResponse(savedPurchase));
            log.debug("Purchase record created: purchaseId={}, bookId={}",
                    savedPurchase.getId(), book.getId());
        }

        // Kullanıcıyı kaydet (ownedBooks güncellemeleri için)
        userRepository.save(user);
        log.debug("User library updated: userId={}, newBookCount={}", userId, validBooks.size());

        // Sepeti temizle
        cart.getItems().clear();
        cartRepository.save(cart);
        log.info("Cart cleared after successful checkout: userId={}", userId);

        // Validation hataları varsa log'a yaz
        if (!validationErrors.isEmpty()) {
            log.warn("Some books were excluded from cart checkout: userId={}, excludedCount={}, reasons={}",
                    userId, validationErrors.size(), String.join(", ", validationErrors));
        }

        log.info("Cart checkout completed: userId={}, purchasedBooks={}, totalAmount={}, transactionId={}",
                userId, successfulPurchases.size(), totalAmount, transactionId);

        return successfulPurchases;
    }

    // Simüle edilmiş ödeme işlemi (ileride gerçek ödeme gateway'i entegre
    // edilebilir)
    private boolean processPayment(Purchase purchase, PurchaseRequest request) {
        log.debug("Processing payment: transactionId={}, amount={}, paymentMethod={}",
                purchase.getTransactionId(), purchase.getPrice(), request.getPaymentMethod());

        // TODO: Gerçek ödeme gateway'i (Stripe, iyzico, vb.) ile entegrasyon
        // Şimdilik her ödemeyi başarılı kabul ediyoruz
        boolean success = true;

        if (success) {
            log.debug("Payment processed successfully: transactionId={}", purchase.getTransactionId());
        } else {
            log.error("Payment processing failed: transactionId={}", purchase.getTransactionId());
        }

        return success;
    }

    // Benzersiz işlem numarası oluştur
    private String generateTransactionId() {
        String transactionId = "TXN-" + UUID.randomUUID().toString();
        log.trace("Generated transaction ID: {}", transactionId);
        return transactionId;
    }

    // Purchase entity'sini PurchaseResponse'a dönüştür
    private PurchaseResponse mapToResponse(Purchase purchase) {
        PurchaseResponse response = new PurchaseResponse();
        response.setId(purchase.getId());
        response.setUserId(purchase.getUser().getId());
        response.setUserName(purchase.getUser().getFirstName() + " " + purchase.getUser().getLastName());
        response.setBookId(purchase.getBook().getId());
        response.setBookTitle(purchase.getBook().getTitle());
        response.setBookCoverImageUrl(purchase.getBook().getCoverImageUrl());
        response.setBookPublisher(purchase.getBook().getPublisher() != null
                ? purchase.getBook().getPublisher().getName()
                : null);
        response.setPrice(purchase.getPrice());
        response.setPurchaseDate(purchase.getPurchaseDate());
        response.setStatus(purchase.getStatus());
        response.setTransactionId(purchase.getTransactionId());
        response.setPaymentMethod(purchase.getPaymentMethod());
        return response;
    }
}