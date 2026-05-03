package com.beratcan.app.service;

import com.beratcan.app.dto.response.DiscountValidationResult;
import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.Cart;
import com.beratcan.app.entity.CartItem;
import com.beratcan.app.entity.DiscountCode;
import com.beratcan.app.entity.User;
import com.beratcan.app.constants.AppTime;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.DiscountCodeNotFoundException;
import com.beratcan.app.repository.DiscountCodeRepository;
import com.beratcan.app.repository.DiscountCodeUsageRepository;
import com.beratcan.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountCodeService {

    private final DiscountCodeRepository discountCodeRepository;
    private final DiscountCodeUsageRepository discountCodeUsageRepository;
    private final UserRepository userRepository;

    /**
     * İndirim kodunu validate et ve uygulanabilir mi kontrol et
     */
    @Transactional(readOnly = true)
    public DiscountValidationResult validateDiscountCode(String code, Long userId, Cart cart) {
        // Case-insensitive ve trim
        final String normalizedCode = code.toUpperCase().trim();

        log.info("Validating discount code: code={}, userId={}", normalizedCode, userId);

        // 1. Kodu bul
        DiscountCode discountCode = discountCodeRepository.findByCode(normalizedCode)
                .orElseThrow(() -> {
                    log.warn("Discount code not found: code={}", normalizedCode);
                    return new BadRequestException("Geçersiz indirim kodu");
                });

        // 2. Aktif mi?
        if (!discountCode.getIsActive()) {
            log.warn("Discount code is not active: code={}", code);
            return DiscountValidationResult.invalid("Bu indirim kodu aktif değil");
        }

        // 3. Tarih kontrolü (Türkiye saatine göre)
        LocalDateTime now = AppTime.now();
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

        // 5.5. Kullanıcıya özel kod kontrolü
        if (discountCode.getApplicableUsers() != null && !discountCode.getApplicableUsers().isEmpty()) {
            boolean isUserApplicable = discountCode.getApplicableUsers().stream()
                    .anyMatch(u -> u.getId().equals(userId));
            if (!isUserApplicable) {
                log.warn("Discount code not applicable to user: userId={}, code={}", userId, normalizedCode);
                return DiscountValidationResult.invalid("Bu indirim kodu size özel tanımlanmamış");
            }
        }

        // 6. Yayınevi/Kitap kontrolü (OR mantığı: ya publisher ya books, ikisi birden değil)
        List<CartItem> applicableItems = getApplicableCartItems(cart, discountCode);
        if (applicableItems.isEmpty()) {
            log.warn("No applicable items for discount code: code={}, cartItems={}",
                    code, cart.getItems().size());
            return DiscountValidationResult.invalid(
                    "Bu indirim kodu sepetinizdeki ürünler için geçerli değil");
        }

        // 7. Sepet tutarı kontrolü (min-max) — sadece uygun ürünlerin toplamına bakılır
        BigDecimal applicableTotal = applicableItems.stream()
                .map(item -> item.getBook().getPrice())
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (discountCode.getMinCartTotal() != null &&
                applicableTotal.compareTo(discountCode.getMinCartTotal()) < 0) {
            log.warn("Applicable items total below minimum: applicableTotal={}, minCartTotal={}",
                    applicableTotal, discountCode.getMinCartTotal());
            return DiscountValidationResult.invalid(
                    "İndirim kodu için minimum tutar: " + discountCode.getMinCartTotal() + " TL " +
                            "(uygun ürünlerin toplamı: " + applicableTotal + " TL)");
        }
        if (discountCode.getMaxCartTotal() != null &&
                applicableTotal.compareTo(discountCode.getMaxCartTotal()) > 0) {
            log.warn("Applicable items total above maximum: applicableTotal={}, maxCartTotal={}",
                    applicableTotal, discountCode.getMaxCartTotal());
            return DiscountValidationResult.invalid(
                    "İndirim kodu için maksimum tutar: " + discountCode.getMaxCartTotal() + " TL");
        }

        // 8. İndirim tutarını hesapla
        BigDecimal discountAmount = calculateDiscount(applicableItems, discountCode);

        log.info("Discount code validated successfully: code={}, discountAmount={}, applicableItems={}",
                code, discountAmount, applicableItems.size());

        return DiscountValidationResult.valid(discountCode, discountAmount, applicableItems);
    }

    /**
     * İndirim koduna uygun sepet öğelerini bul
     * OR mantığı: ya publisher ya books filtresi uygulanır, ikisi birden değil.
     * İkisi de boşsa tüm kitaplar geçerlidir.
     */
    private List<CartItem> getApplicableCartItems(Cart cart, DiscountCode code) {
        boolean hasPublisherFilter = code.getApplicablePublishers() != null && !code.getApplicablePublishers().isEmpty();
        boolean hasBookFilter = code.getApplicableBooks() != null && !code.getApplicableBooks().isEmpty();

        // İkisi de boşsa → tüm kitaplar geçerli
        if (!hasPublisherFilter && !hasBookFilter) {
            return cart.getItems().stream().collect(Collectors.toList());
        }

        return cart.getItems().stream()
                .filter(item -> {
                    Book book = item.getBook();

                    // Publisher filtresi varsa → publisher set'inde mi
                    if (hasPublisherFilter) {
                        return book.getPublisher() != null &&
                                code.getApplicablePublishers().stream()
                                        .anyMatch(p -> p.getId().equals(
                                                book.getPublisher().getId()));
                    }

                    // Book filtresi varsa → kitap listesinde mi
                    return code.getApplicableBooks().stream()
                            .anyMatch(b -> b.getId().equals(book.getId()));
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
     * İndirim kodunu kullan (checkout sırasında)
     * NOT: Bu metod purchase kayıtları başarıyla oluşturulduktan SONRA çağrılmalı
     */
    @Transactional
    public void useDiscountCode(Long discountCodeId, Long userId, String transactionId) {
        log.info("Using discount code: discountCodeId={}, userId={}, transactionId={}",
                discountCodeId, userId, transactionId);

        DiscountCode code = discountCodeRepository.findById(discountCodeId)
                .orElseThrow(() -> new DiscountCodeNotFoundException(discountCodeId));

        // Son limit kontrolü (race condition önleme)
        if (code.getUsedCount() >= code.getUsageLimit()) {
            log.error("Discount code usage limit reached during use: codeId={}, usedCount={}, limit={}",
                    discountCodeId, code.getUsedCount(), code.getUsageLimit());
            throw new BadRequestException("Bu indirim kodu kullanım limitine ulaştı");
        }

        // Kullanım sayısını artır (optimistic locking ile korunuyor)
        try {
            code.setUsedCount(code.getUsedCount() + 1);
            discountCodeRepository.save(code);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic lock conflict for discount code: codeId={}, code={}",
                    discountCodeId, code.getCode());
            throw new BadRequestException(
                    "İndirim kodu aynı anda başka biri tarafından kullanıldı, lütfen tekrar deneyin");
        }

        // Kullanım kaydı oluştur
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Kullanıcı bulunamadı. userId=" + userId));

        com.beratcan.app.entity.DiscountCodeUsage usage = new com.beratcan.app.entity.DiscountCodeUsage();
        usage.setDiscountCode(code);
        usage.setUser(user);
        usage.setUsedAt(AppTime.now());
        usage.setTransactionId(transactionId);
        discountCodeUsageRepository.save(usage);

        log.info("Discount code used successfully: code={}, newUsedCount={}",
                code.getCode(), code.getUsedCount());
    }

}
