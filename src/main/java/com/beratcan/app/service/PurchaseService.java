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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;

    @Transactional
    public PurchaseResponse buyBook(Long userId, Long bookId, PurchaseRequest request) {
        // Kullanıcıyı getir
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        // Kitabı getir
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Kitap bulunamadı"));

        // Kitap ücretsiz mi kontrol et
        if (!book.isPaid()) {
            throw new RuntimeException("Bu kitap zaten ücretsizdir");
        }

        // Kitabın fiyatı var mı kontrol et
        if (book.getPrice() == null || book.getPrice().signum() <= 0) {
            throw new RuntimeException("Kitap fiyatı geçerli değil");
        }

        // Kullanıcı bu kitabı daha önce satın almış mı kontrol et
        boolean alreadyOwned = user.getOwnedBooks().stream()
                .anyMatch(b -> b.getId().equals(bookId));

        if (alreadyOwned) {
            throw new RuntimeException("Bu kitap zaten kütüphanenizde mevcut");
        }

        // Tamamlanmış bir satın alma kaydı var mı kontrol et
        purchaseRepository.findByUserIdAndBookIdAndStatus(userId, bookId, PurchaseStatus.COMPLETED)
                .ifPresent(p -> {
                    throw new RuntimeException("Bu kitabı zaten satın almışsınız");
                });

        // Purchase kaydı oluştur
        Purchase purchase = new Purchase();
        purchase.setUser(user);
        purchase.setBook(book);
        purchase.setPrice(book.getPrice());
        purchase.setPurchaseDate(LocalDateTime.now());
        purchase.setPaymentMethod(request.getPaymentMethod());
        purchase.setTransactionId(generateTransactionId());

        // Burada gerçek ödeme işlemi yapılacak
        // Şimdilik her satın alma işlemini başarılı kabul ediyoruz
        boolean paymentSuccess = processPayment(purchase, request);

        if (paymentSuccess) {
            purchase.setStatus(PurchaseStatus.COMPLETED);

            // Kitabı kullanıcının kütüphanesine ekle
            user.getOwnedBooks().add(book);
            userRepository.save(user);
        } else {
            purchase.setStatus(PurchaseStatus.FAILED);
        }

        // Purchase kaydını kaydet
        Purchase savedPurchase = purchaseRepository.save(purchase);

        return mapToResponse(savedPurchase);
    }

    public List<PurchaseResponse> getUserPurchases(Long userId) {
        List<Purchase> purchases = purchaseRepository.findByUserIdOrderByPurchaseDateDesc(userId);
        return purchases.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<PurchaseResponse> getUserCompletedPurchases(Long userId) {
        List<Purchase> purchases = purchaseRepository
                .findByUserIdAndStatusOrderByPurchaseDateDesc(userId, PurchaseStatus.COMPLETED);
        return purchases.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public PurchaseResponse getPurchaseById(Long purchaseId, Long userId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new RuntimeException("Satın alma kaydı bulunamadı"));

        // Kullanıcının kendi satın alma kaydını görmesini sağla
        if (!purchase.getUser().getId().equals(userId)) {
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
        // Kullanıcıyı getir
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        // Sepeti getir
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Sepet bulunamadı"));

        // Sepet boş mu kontrol et
        if (cart.getItems().isEmpty()) {
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

        // Hiç geçerli kitap yoksa hata fırlat
        if (validBooks.isEmpty()) {
            throw new RuntimeException("Sepette satın alınabilir kitap yok: " + 
                String.join(", ", validationErrors));
        }

        // ADIM 2: TEK SEFERDE tüm sepet için ödeme işlemini yap
        String transactionId = generateTransactionId();
        PurchaseRequest paymentRequest = new PurchaseRequest();
        paymentRequest.setPaymentMethod("Sepet ile satın alma");

        // Dummy purchase objesi oluştur (sadece processPayment için)
        Purchase dummyPurchase = new Purchase();
        dummyPurchase.setPrice(totalAmount); // Toplam tutar
        dummyPurchase.setTransactionId(transactionId);
        
        boolean paymentSuccess = processPayment(dummyPurchase, paymentRequest);

        // ADIM 3: Ödeme başarısız ise hiçbir şey yapma
        if (!paymentSuccess) {
            throw new RuntimeException("Ödeme işlemi başarısız oldu. Toplam tutar: " + 
                totalAmount + " TL. Lütfen tekrar deneyin.");
        }

        // ADIM 4: Ödeme başarılı - Tüm kitaplar için Purchase kayıtları oluştur
        List<PurchaseResponse> successfulPurchases = new ArrayList<>();
        LocalDateTime purchaseDate = LocalDateTime.now();

        for (Book book : validBooks) {
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
        }

        // Kullanıcıyı kaydet (ownedBooks güncellemeleri için)
        userRepository.save(user);

        // Sepeti temizle
        cart.getItems().clear();
        cartRepository.save(cart);

        // Validation hataları varsa log'a yaz
        if (!validationErrors.isEmpty()) {
            System.out.println("Bazı kitaplar sepetten çıkarıldı: " + 
                String.join(", ", validationErrors));
        }

        return successfulPurchases;
    }

    // Simüle edilmiş ödeme işlemi (ileride gerçek ödeme gateway'i entegre
    // edilebilir)
    private boolean processPayment(Purchase purchase, PurchaseRequest request) {
        // TODO: Gerçek ödeme gateway'i (Stripe, iyzico, vb.) ile entegrasyon
        // Şimdilik her ödemeyi başarılı kabul ediyoruz
        return true;
    }

    // Benzersiz işlem numarası oluştur
    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString();
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
