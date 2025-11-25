package com.beratcan.app.service;

import com.beratcan.app.dto.request.DiscountCodeRequest;
import com.beratcan.app.dto.response.DiscountCodeResponse;
import com.beratcan.app.dto.response.DiscountValidationResult;
import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.Cart;
import com.beratcan.app.entity.CartItem;
import com.beratcan.app.entity.DiscountCode;
import com.beratcan.app.entity.Publisher;
import com.beratcan.app.entity.User;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.ConflictException;
import com.beratcan.app.exception.DiscountCodeNotFoundException;
import com.beratcan.app.repository.BookRepository;
import com.beratcan.app.repository.DiscountCodeRepository;
import com.beratcan.app.repository.DiscountCodeUsageRepository;
import com.beratcan.app.repository.PublisherRepository;
import com.beratcan.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountCodeService {

    private final DiscountCodeRepository discountCodeRepository;
    private final DiscountCodeUsageRepository discountCodeUsageRepository;
    private final UserRepository userRepository;
    private final PublisherRepository publisherRepository;
    private final BookRepository bookRepository;

    /**
     * İndirim kodunu validate et ve uygulanabilir mi kontrol et
     */
    @Transactional(readOnly = true)
    public DiscountValidationResult validateDiscountCode(String code, Long userId, Cart cart) {
        log.info("Validating discount code: code={}, userId={}", code, userId);

        // 1. Kodu bul
        DiscountCode discountCode = discountCodeRepository.findByCode(code)
                .orElseThrow(() -> {
                    log.warn("Discount code not found: code={}", code);
                    return new BadRequestException("Geçersiz indirim kodu");
                });

        // 2. Aktif mi?
        if (!discountCode.getIsActive()) {
            log.warn("Discount code is not active: code={}", code);
            return DiscountValidationResult.invalid("Bu indirim kodu aktif değil");
        }

        // 3. Tarih kontrolü
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(discountCode.getValidFrom())) {
            log.warn("Discount code not yet valid: code={}, validFrom={}", code, discountCode.getValidFrom());
            return DiscountValidationResult.invalid("Bu indirim kodu henüz geçerli değil");
        }
        if (now.isAfter(discountCode.getValidUntil())) {
            log.warn("Discount code expired: code={}, validUntil={}", code, discountCode.getValidUntil());
            return DiscountValidationResult.invalid("Bu indirim kodu süresi dolmuş");
        }

        // 4. Kullanım limiti kontrolü
        if (discountCode.getUsedCount() >= discountCode.getUsageLimit()) {
            log.warn("Discount code usage limit reached: code={}, usedCount={}, limit={}",
                    code, discountCode.getUsedCount(), discountCode.getUsageLimit());
            return DiscountValidationResult.invalid("Bu indirim kodu kullanım limitine ulaştı");
        }

        // 5. Kullanıcı başına limit kontrolü
        if (discountCode.getMaxUsagePerUser() != null) {
            long userUsageCount = discountCodeUsageRepository
                    .countByDiscountCodeIdAndUserId(discountCode.getId(), userId);
            if (userUsageCount >= discountCode.getMaxUsagePerUser()) {
                log.warn("User reached max usage for discount code: userId={}, code={}, usageCount={}, max={}",
                        userId, code, userUsageCount, discountCode.getMaxUsagePerUser());
                return DiscountValidationResult.invalid(
                        "Bu indirim kodunu maksimum kullanım sayısına ulaştınız");
            }
        }

        // 6. Sepet tutarı kontrolü (min-max)
        BigDecimal cartTotal = calculateCartTotal(cart);
        if (discountCode.getMinCartTotal() != null &&
                cartTotal.compareTo(discountCode.getMinCartTotal()) < 0) {
            log.warn("Cart total below minimum: cartTotal={}, minCartTotal={}",
                    cartTotal, discountCode.getMinCartTotal());
            return DiscountValidationResult.invalid(
                    "Minimum sepet tutarı: " + discountCode.getMinCartTotal() + " TL");
        }
        if (discountCode.getMaxCartTotal() != null &&
                cartTotal.compareTo(discountCode.getMaxCartTotal()) > 0) {
            log.warn("Cart total above maximum: cartTotal={}, maxCartTotal={}",
                    cartTotal, discountCode.getMaxCartTotal());
            return DiscountValidationResult.invalid(
                    "Maksimum sepet tutarı: " + discountCode.getMaxCartTotal() + " TL");
        }

        // 7. Yayınevi/Kitap kontrolü
        List<CartItem> applicableItems = getApplicableCartItems(cart, discountCode);
        if (applicableItems.isEmpty()) {
            log.warn("No applicable items for discount code: code={}, cartItems={}",
                    code, cart.getItems().size());
            return DiscountValidationResult.invalid(
                    "Bu indirim kodu sepetinizdeki ürünler için geçerli değil");
        }

        // 8. İndirim tutarını hesapla
        BigDecimal discountAmount = calculateDiscount(applicableItems, discountCode);

        log.info("Discount code validated successfully: code={}, discountAmount={}, applicableItems={}",
                code, discountAmount, applicableItems.size());

        return DiscountValidationResult.valid(discountCode, discountAmount, applicableItems);
    }

    /**
     * İndirim koduna uygun sepet öğelerini bul
     */
    private List<CartItem> getApplicableCartItems(Cart cart, DiscountCode code) {
        return cart.getItems().stream()
                .filter(item -> {
                    Book book = item.getBook();

                    // Yayınevi kontrolü
                    if (code.getApplicablePublisher() != null) {
                        if (book.getPublisher() == null ||
                                !book.getPublisher().getId().equals(code.getApplicablePublisher().getId())) {
                            return false;
                        }
                    }

                    // Kitap kontrolü
                    if (code.getApplicableBooks() != null && !code.getApplicableBooks().isEmpty()) {
                        boolean isApplicable = code.getApplicableBooks().stream()
                                .anyMatch(b -> b.getId().equals(book.getId()));
                        if (!isApplicable) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * İndirim tutarını hesapla
     */
    private BigDecimal calculateDiscount(List<CartItem> applicableItems, DiscountCode code) {
        BigDecimal applicableTotal = applicableItems.stream()
                .map(item -> item.getBook().getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (code.getDiscountType() == com.beratcan.app.entity.enums.DiscountType.PERCENTAGE) {
            // %30 indirim
            return applicableTotal
                    .multiply(code.getDiscountValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else {
            // Sabit tutar (max applicableTotal kadar)
            return code.getDiscountValue().min(applicableTotal);
        }
    }

    /**
     * Sepet toplamını hesapla
     */
    private BigDecimal calculateCartTotal(Cart cart) {
        return cart.getItems().stream()
                .map(item -> item.getBook().getPrice())
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * İndirim kodunu kullan (checkout sırasında)
     */
    @Transactional
    public void useDiscountCode(Long discountCodeId, Long userId, String transactionId) {
        log.info("Using discount code: discountCodeId={}, userId={}, transactionId={}",
                discountCodeId, userId, transactionId);

        DiscountCode code = discountCodeRepository.findById(discountCodeId)
                .orElseThrow(() -> new DiscountCodeNotFoundException(discountCodeId));

        // Kullanım sayısını artır
        code.setUsedCount(code.getUsedCount() + 1);
        discountCodeRepository.save(code);

        // Kullanım kaydı oluştur
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.beratcan.app.exception.UserNotFoundException(userId));

        com.beratcan.app.entity.DiscountCodeUsage usage = new com.beratcan.app.entity.DiscountCodeUsage();
        usage.setDiscountCode(code);
        usage.setUser(user);
        usage.setUsedAt(LocalDateTime.now());
        usage.setTransactionId(transactionId);
        discountCodeUsageRepository.save(usage);

        log.info("Discount code used successfully: code={}, newUsedCount={}",
                code.getCode(), code.getUsedCount());
    }

    /**
     * Admin: İndirim kodu oluştur
     */
    @Transactional
    public DiscountCodeResponse createDiscountCode(DiscountCodeRequest request) {
        log.info("Creating discount code: code={}", request.getCode());

        // Kod zaten var mı?
        if (discountCodeRepository.existsByCode(request.getCode())) {
            throw new ConflictException("Bu indirim kodu zaten mevcut: " + request.getCode());
        }

        DiscountCode discountCode = new DiscountCode();
        discountCode.setCode(request.getCode().toUpperCase());
        discountCode.setDescription(request.getDescription());
        discountCode.setDiscountType(request.getDiscountType());
        discountCode.setDiscountValue(request.getDiscountValue());
        discountCode.setUsageLimit(request.getUsageLimit());
        discountCode.setMinCartTotal(request.getMinCartTotal());
        discountCode.setMaxCartTotal(request.getMaxCartTotal());
        discountCode.setValidFrom(request.getValidFrom());
        discountCode.setValidUntil(request.getValidUntil());
        discountCode.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        discountCode.setMaxUsagePerUser(request.getMaxUsagePerUser());

        // Yayınevi
        if (request.getApplicablePublisherId() != null) {
            Publisher publisher = publisherRepository.findById(request.getApplicablePublisherId())
                    .orElseThrow(() -> new com.beratcan.app.exception.PublisherNotFoundException(
                            request.getApplicablePublisherId()));
            discountCode.setApplicablePublisher(publisher);
        }

        // Kitaplar
        if (request.getApplicableBookIds() != null && !request.getApplicableBookIds().isEmpty()) {
            Set<Book> books = request.getApplicableBookIds().stream()
                    .map(bookId -> bookRepository.findById(bookId)
                            .orElseThrow(() -> new com.beratcan.app.exception.BookNotFoundException(bookId)))
                    .collect(Collectors.toSet());
            discountCode.setApplicableBooks(books);
        }

        discountCode = discountCodeRepository.save(discountCode);
        log.info("Discount code created successfully: id={}, code={}", discountCode.getId(), discountCode.getCode());

        return toDiscountCodeResponse(discountCode);
    }

    /**
     * DiscountCode'u DiscountCodeResponse'a dönüştür
     */
    private DiscountCodeResponse toDiscountCodeResponse(DiscountCode code) {
        DiscountCodeResponse response = new DiscountCodeResponse();
        response.setId(code.getId());
        response.setCode(code.getCode());
        response.setDescription(code.getDescription());
        response.setDiscountType(code.getDiscountType());
        response.setDiscountValue(code.getDiscountValue());
        response.setUsageLimit(code.getUsageLimit());
        response.setUsedCount(code.getUsedCount());
        response.setMinCartTotal(code.getMinCartTotal());
        response.setMaxCartTotal(code.getMaxCartTotal());
        response.setValidFrom(code.getValidFrom());
        response.setValidUntil(code.getValidUntil());
        response.setIsActive(code.getIsActive());
        response.setMaxUsagePerUser(code.getMaxUsagePerUser());
        response.setCreatedAt(code.getCreatedAt());
        response.setUpdatedAt(code.getUpdatedAt());

        if (code.getApplicablePublisher() != null) {
            response.setApplicablePublisherName(code.getApplicablePublisher().getName());
        }
        if (code.getApplicableBooks() != null) {
            response.setApplicableBookCount(code.getApplicableBooks().size());
        }

        return response;
    }
}
