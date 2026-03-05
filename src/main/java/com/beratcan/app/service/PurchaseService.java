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
        // JOIN FETCH ile User, Book, Publisher ve DiscountCode'u tek query'de yükle
        Purchase purchase = purchaseRepository.findByIdWithUserAndBook(purchaseId)
                .orElseThrow(() -> {
                    PurchaseNotFoundException ex = new PurchaseNotFoundException(purchaseId);
                    log.error("Purchase not found: purchaseId={}, userId={}", purchaseId, userId, ex);
                    return ex;
                });

        // Kullanıcının kendi satın alma kaydını görmesini sağla (User zaten JOIN FETCH
        // ile yüklü)
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
        long startTime = System.currentTimeMillis();

        log.info("EVENT: Cart checkout started | userId={}", userId);

        // Kullanıcıyı getir
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    UserNotFoundException ex = new UserNotFoundException(userId);
                    log.error("User not found for cart checkout: userId={}", userId, ex);
                    return ex;
                });

        // Sepeti getir (pessimistic lock ile - concurrent checkout'u önlemek için)
        // JOIN FETCH ile tüm ilişkileri yükle (N+1 query önleme)
        Cart cart = cartRepository.findByUserIdWithLockAndItems(userId)
                .orElseThrow(() -> {
                    CartNotFoundException ex = new CartNotFoundException(userId);
                    log.error("Cart not found for user: userId={}", userId, ex);
                    return ex;
                });

        int cartItemCount = cart.getItems().size();
        BigDecimal initialTotalAmount = cart.getItems().stream()
                .map(item -> item.getPriceAtAdd() != null ? item.getPriceAtAdd() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("EVENT: Cart checkout details | userId={} | itemCount={} | initialTotalAmount={}",
                userId, cartItemCount, initialTotalAmount);

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
            Book bookFromCart = cartItem.getBook();

            // 1. Kitap varlık kontrolü - Kitap silinmiş olabilir
            if (bookFromCart == null) {
                validationErrors.add("Sepetteki bir kitap artık mevcut değil (silinmiş)");
                log.warn("Book not found in cart item: cartItemId={}, userId={}", cartItem.getId(), userId);
                continue;
            }

            // 2. Kitap ID ile tekrar kontrol et (lazy loading sorunları ve silinme kontrolü
            // için)
            final Book book;
            try {
                Book verifiedBook = bookRepository.findById(bookFromCart.getId())
                        .orElse(null);
                if (verifiedBook == null) {
                    String bookTitle = bookFromCart.getTitle() != null ? bookFromCart.getTitle() : "Bilinmeyen kitap";
                    validationErrors.add(bookTitle + ": Kitap artık mevcut değil (silinmiş)");
                    log.warn("Book deleted after being added to cart: bookId={}, userId={}", bookFromCart.getId(),
                            userId);
                    continue;
                }
                // Verified book'u kullan (güncel veri için)
                book = verifiedBook;
            } catch (Exception e) {
                log.error("Error verifying book existence: bookId={}, userId={}", bookFromCart.getId(), userId, e);
                String bookTitle = bookFromCart.getTitle() != null ? bookFromCart.getTitle() : "Bilinmeyen kitap";
                validationErrors.add(bookTitle + ": Kitap kontrol edilemedi");
                continue;
            }

            // 3. Fiyat değişikliği kontrolü
            BigDecimal priceAtAdd = cartItem.getPriceAtAdd();
            BigDecimal currentPrice = book.getPrice();

            if (priceAtAdd != null && currentPrice != null &&
                    priceAtAdd.compareTo(currentPrice) != 0) {
                validationErrors.add(book.getTitle() +
                        ": Fiyat değişti (Sepete eklendiğinde: " + priceAtAdd + " TL, Şimdi: " + currentPrice + " TL)");
                log.warn("Price changed for book in cart: bookId={}, priceAtAdd={}, currentPrice={}, userId={}",
                        book.getId(), priceAtAdd, currentPrice, userId);
                continue;
            }

            // 4. Validasyonlar
            if (!book.isPaid()) {
                validationErrors.add(book.getTitle() + ": Ücretsiz kitap");
                continue;
            }

            if (book.getPrice() == null || book.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                validationErrors.add(book.getTitle() + ": Geçersiz fiyat");
                continue;
            }

            // 5. Zaten sahip mi kontrol et
            final Long bookId = book.getId();
            boolean alreadyOwned = user.getOwnedBooks().stream()
                    .anyMatch(b -> b.getId().equals(bookId));
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
        String discountCodeWarning = null; // İndirim kodu uyarısı için

        if (cart.getAppliedDiscountCode() != null) {
            // İndirim kodunu tekrar validate et (son kontrol)
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
                // İndirim geçersiz, kullanıcıyı bilgilendir
                discountCodeWarning = "İndirim kodu geçersiz: " + validation.getErrorMessage() +
                        " Ödeme indirimsiz olarak devam edecek.";
                // İndirim bilgilerini temizle
                cart.setAppliedDiscountCode(null);
                cart.setDiscountAmount(null);
                cart.setTotalPriceAfterDiscount(null);
            }
        }

        // ADIM 3: Önce Purchase kayıtlarını PENDING olarak oluştur (güvenli yaklaşım)
        // Bu sayede ödeme başarısız olursa transaction rollback olur ve kayıtlar FAILED
        // olarak kalır
        String transactionId = generateTransactionId();
        List<Purchase> pendingPurchases = new ArrayList<>();
        LocalDateTime purchaseDate = LocalDateTime.now();

        // İndirim tutarını kitaplara orantılı dağıt (accumulator pattern - rounding sorununu çözer)
        // Her kitabın payı = (kitapFiyatı / toplamFiyat) * indirimTutarı
        BigDecimal totalDiscountDistributed = BigDecimal.ZERO;

        int bookIndex = 0;
        for (Book book : validBooks) {

            BigDecimal bookDiscount = BigDecimal.ZERO;
            boolean isLastBook = (bookIndex == validBooks.size() - 1);

            if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                if (isLastBook) {
                    // Son kitaba kalan farkı ekle (rounding sorununu çözer)
                    bookDiscount = discountAmount.subtract(totalDiscountDistributed);
                } else {
                    // Orantılı indirim: (kitapFiyatı / toplamFiyat) * indirimTutarı
                    bookDiscount = book.getPrice()
                            .divide(totalAmount, 10, RoundingMode.HALF_UP)
                            .multiply(discountAmount)
                            .setScale(2, RoundingMode.HALF_UP);
                    totalDiscountDistributed = totalDiscountDistributed.add(bookDiscount);
                }
            }

            BigDecimal bookFinalPrice = book.getPrice().subtract(bookDiscount);
            // Negatif fiyat koruması
            if (bookFinalPrice.compareTo(BigDecimal.ZERO) < 0) {
                bookFinalPrice = BigDecimal.ZERO;
            }

            Purchase purchase = new Purchase();
            purchase.setUser(user);
            purchase.setBook(book);
            purchase.setPrice(book.getPrice()); // İndirim öncesi fiyat
            purchase.setPurchaseDate(purchaseDate);
            purchase.setPaymentMethod("Sepet ile satın alma");
            purchase.setTransactionId(transactionId); // Aynı transaction ID
            purchase.setStatus(PurchaseStatus.PENDING); // Önce PENDING olarak oluştur (ödeme yapılmadan önce)

            // İndirim bilgileri
            if (cart.getAppliedDiscountCode() != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                purchase.setDiscountCode(cart.getAppliedDiscountCode());
                purchase.setDiscountAmount(bookDiscount);
                purchase.setFinalPrice(bookFinalPrice);
            } else {
                purchase.setFinalPrice(book.getPrice()); // İndirim yoksa normal fiyat
            }

            // Purchase'ı PENDING olarak kaydet
            Purchase savedPurchase = purchaseRepository.save(purchase);
            pendingPurchases.add(savedPurchase); // Geçici olarak listeye ekle

            bookIndex++; // Sonraki kitap için index'i artır
        }

        // Kullanıcıyı kaydet (ownedBooks güncellemeleri için - henüz kitap eklenmedi)
        userRepository.save(user);

        // ADIM 5: Şimdi ödeme işlemini yap
        PurchaseRequest paymentRequest = new PurchaseRequest();
        paymentRequest.setPaymentMethod("Sepet ile satın alma");

        // Dummy purchase objesi oluştur (sadece processPayment için)
        Purchase dummyPurchase = new Purchase();
        dummyPurchase.setPrice(finalTotalAmount); // İndirim sonrası toplam tutar
        dummyPurchase.setTransactionId(transactionId);

        log.info("Processing payment: userId={}, finalTotalAmount={}, transactionId={}",
                userId, finalTotalAmount, transactionId);

        boolean paymentSuccess = processPayment(dummyPurchase, paymentRequest);

        // ADIM 6: Ödeme sonucuna göre purchase kayıtlarını güncelle
        List<PurchaseResponse> purchaseResponses = new ArrayList<>();

        if (paymentSuccess) {
            log.info("Payment successful: userId={}, finalTotalAmount={}, transactionId={}",
                    userId, finalTotalAmount, transactionId);

            // Ödeme başarılı - Tüm purchase kayıtlarını COMPLETED yap ve kütüphaneye ekle
            for (int i = 0; i < pendingPurchases.size(); i++) {
                Purchase purchase = pendingPurchases.get(i);
                Book book = validBooks.get(i);

                purchase.setStatus(PurchaseStatus.COMPLETED);

                // Kitabı kullanıcının kütüphanesine ekle
                try {
                    user.getOwnedBooks().add(book);
                } catch (DataIntegrityViolationException e) {
                    // DB constraint hatası (duplicate entry) yakalandı
                    if (e.getMessage() != null && e.getMessage().contains("uk_user_book")) {
                        log.warn("Book already in library (checkout): userId={}, bookId={}", userId, book.getId());
                        // Kitap zaten kütüphanede, purchase kaydı oluşturmaya devam et
                    } else {
                        throw e; // Diğer constraint hataları için
                    }
                }

                // Purchase'ı güncelle
                Purchase updatedPurchase = purchaseRepository.save(purchase);
                PurchaseResponse purchaseResponse = mapToResponse(updatedPurchase);
                // İndirim kodu uyarısı varsa ekle (sadece ilk purchase'a)
                if (discountCodeWarning != null && i == 0) {
                    purchaseResponse.setInfoMessage(discountCodeWarning);
                }
                purchaseResponses.add(purchaseResponse);
            }

            // Kullanıcıyı kaydet (ownedBooks güncellemeleri için)
            userRepository.save(user);

            // İndirim kodunu kullan (kaydet) - Purchase kayıtları başarıyla oluşturulduktan
            // SONRA
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

            // Ödeme başarısız - Tüm purchase kayıtlarını FAILED yap
            for (Purchase purchase : pendingPurchases) {
                purchase.setStatus(PurchaseStatus.FAILED);
                purchaseRepository.save(purchase);
            }

            // Transaction rollback olacak ama purchase kayıtları FAILED olarak kalacak
            // (gerçek ödeme gateway'i entegre edilirken refund işlemi yapılmalı)
            throw new ExternalServiceException("Ödeme işlemi başarısız oldu. Toplam tutar: " +
                    finalTotalAmount + " TL. Lütfen tekrar deneyin.");
        }

        // Sepeti temizle (items ve indirim kodu bilgileri)
        cart.getItems().clear();
        cart.setAppliedDiscountCode(null);
        cart.setDiscountAmount(null);
        cart.setTotalPriceAfterDiscount(null);
        cartRepository.save(cart);
        log.info("Cart cleared after successful checkout: userId={}", userId);

        // Validation hataları varsa log'a yaz
        if (!validationErrors.isEmpty()) {
            log.warn("Some books were excluded from cart checkout: userId={}, excludedCount={}, reasons={}",
                    userId, validationErrors.size(), String.join(", ", validationErrors));
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info(
                "EVENT: Cart checkout completed | userId={} | purchaseCount={} | totalAmount={} | transactionId={} | duration={}ms",
                userId, purchaseResponses.size(), finalTotalAmount, transactionId, duration);

        // Performance warning for slow operations (5 second threshold)
        if (duration > 5000) {
            log.warn("PERF: Cart checkout slow | duration={}ms | threshold=5000ms | userId={} | transactionId={}",
                    duration, userId, transactionId);
        }

        return purchaseResponses;
    }

    // Simüle edilmiş ödeme işlemi (ileride gerçek ödeme gateway'i entegre
    // edilebilir)
    private boolean processPayment(Purchase purchase, PurchaseRequest request) {

        // TODO: Gerçek ödeme gateway'i (Stripe, iyzico, vb.) ile entegrasyon
        // Şimdilik her ödemeyi başarılı kabul ediyoruz
        boolean success = true;

        if (success) {
        } else {
            log.error("Payment processing failed: transactionId={}", purchase.getTransactionId());
        }

        return success;
    }

    // Benzersiz işlem numarası oluştur
    private String generateTransactionId() {
        String transactionId = "TXN-" + UUID.randomUUID().toString();
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

        // İndirim bilgilerini ekle
        if (purchase.getDiscountCode() != null) {
            response.setDiscountCode(purchase.getDiscountCode().getCode());
        }
        response.setDiscountAmount(purchase.getDiscountAmount());
        response.setFinalPrice(purchase.getFinalPrice());

        return response;
    }
}