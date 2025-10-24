package com.beratcan.app.service;

import com.beratcan.app.dto.request.PurchaseRequest;
import com.beratcan.app.dto.response.PurchaseResponse;
import com.beratcan.app.entity.*;
import com.beratcan.app.entity.enums.PurchaseStatus;
import com.beratcan.app.repository.BookRepository;
import com.beratcan.app.repository.CartRepository;
import com.beratcan.app.repository.PurchaseRepository;
import com.beratcan.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    @Transactional
    public PurchaseResponse buyBook(Long userId, Long bookId, PurchaseRequest request) {
        log.info("Starting book purchase: userId={}, bookId={}, paymentMethod={}",
                userId, bookId, request.getPaymentMethod());

        // Kullanıcıyı getir
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    RuntimeException ex = new RuntimeException("Kullanıcı bulunamadı");
                    log.error("User not found for purchase: userId={}", userId, ex);
                    return ex;
                });

        // Kitabı getir
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> {
                    RuntimeException ex = new RuntimeException("Kitap bulunamadı");
                    log.error("Book not found for purchase: bookId={}, userId={}", bookId, userId, ex);
                    return ex;
                });

        // Kitap ücretsiz mi kontrol et
        if (!book.isPaid()) {
            log.warn("Attempted to purchase free book: userId={}, bookId={}, title={}",
                    userId, bookId, book.getTitle());
            throw new RuntimeException("Bu kitap zaten ücretsizdir");
        }

        // Kitabın fiyatı var mı kontrol et
        if (book.getPrice() == null || book.getPrice().signum() <= 0) {
            log.error("Invalid book price: bookId={}, price={}", bookId, book.getPrice());
            throw new RuntimeException("Kitap fiyatı geçerli değil");
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
            user.getOwnedBooks().add(book);
            userRepository.save(user);
            log.debug("Book added to user library: userId={}, bookId={}", userId, bookId);
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
                    RuntimeException ex = new RuntimeException("Satın alma kaydı bulunamadı");
                    log.error("Purchase not found: purchaseId={}, userId={}", purchaseId, userId, ex);
                    return ex;
                });

        // Kullanıcının kendi satın alma kaydını görmesini sağla
        if (!purchase.getUser().getId().equals(userId)) {
            log.warn("Unauthorized purchase access attempt: purchaseId={}, requestingUserId={}, ownerUserId={}",
                    purchaseId, userId, purchase.getUser().getId());
            throw new RuntimeException("Bu satın alma kaydına erişim yetkiniz yok");
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
                    RuntimeException ex = new RuntimeException("Kullanıcı bulunamadı");
                    log.error("User not found for cart checkout: userId={}", userId, ex);
                    return ex;
                });

        // Sepeti getir
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    RuntimeException ex = new RuntimeException("Sepet bulunamadı");
                    log.error("Cart not found for user: userId={}", userId, ex);
                    return ex;
                });

        int cartItemCount = cart.getItems().size();

        // Sepet boş mu kontrol et
        if (cart.getItems().isEmpty()) {
            log.warn("Attempted to checkout empty cart: userId={}", userId);
            throw new RuntimeException("Sepetiniz boş");
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
            throw new RuntimeException("Sepette satın alınabilir kitap yok: " +
                    String.join(", ", validationErrors));
        }

        // ADIM 2: TEK SEFERDE tüm sepet için ödeme işlemini yap
        String transactionId = generateTransactionId();
        log.info("Processing bulk payment: userId={}, bookCount={}, totalAmount={}, transactionId={}",
                userId, validBooks.size(), totalAmount, transactionId);

        PurchaseRequest paymentRequest = new PurchaseRequest();
        paymentRequest.setPaymentMethod("Sepet ile satın alma");

        // Dummy purchase objesi oluştur (sadece processPayment için)
        Purchase dummyPurchase = new Purchase();
        dummyPurchase.setPrice(totalAmount); // Toplam tutar
        dummyPurchase.setTransactionId(transactionId);

        boolean paymentSuccess = processPayment(dummyPurchase, paymentRequest);

        // ADIM 3: Ödeme başarısız ise hiçbir şey yapma
        if (!paymentSuccess) {
            log.error("Bulk payment failed: userId={}, totalAmount={}, transactionId={}",
                    userId, totalAmount, transactionId);
            throw new RuntimeException("Ödeme işlemi başarısız oldu. Toplam tutar: " +
                    totalAmount + " TL. Lütfen tekrar deneyin.");
        }

        log.info("Bulk payment successful: userId={}, totalAmount={}, transactionId={}",
                userId, totalAmount, transactionId);

        // ADIM 4: Ödeme başarılı - Tüm kitaplar için Purchase kayıtları oluştur
        List<PurchaseResponse> successfulPurchases = new ArrayList<>();
        LocalDateTime purchaseDate = LocalDateTime.now();

        for (Book book : validBooks) {
            log.debug("Creating purchase record: userId={}, bookId={}, transactionId={}",
                    userId, book.getId(), transactionId);

            Purchase purchase = new Purchase();
            purchase.setUser(user);
            purchase.setBook(book);
            purchase.setPrice(book.getPrice());
            purchase.setPurchaseDate(purchaseDate);
            purchase.setPaymentMethod("Sepet ile satın alma");
            purchase.setTransactionId(transactionId); // Aynı transaction ID
            purchase.setStatus(PurchaseStatus.COMPLETED);

            // Kitabı kullanıcının kütüphanesine ekle
            user.getOwnedBooks().add(book);

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
        response.setPrice(purchase.getPrice());
        response.setPurchaseDate(purchase.getPurchaseDate());
        response.setStatus(purchase.getStatus());
        response.setTransactionId(purchase.getTransactionId());
        response.setPaymentMethod(purchase.getPaymentMethod());
        return response;
    }
}