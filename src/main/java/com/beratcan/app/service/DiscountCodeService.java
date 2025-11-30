package com.beratcan.app.service;

import com.beratcan.app.dto.response.DiscountValidationResult;
import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.Cart;
import com.beratcan.app.entity.CartItem;
import com.beratcan.app.entity.DiscountCode;
import com.beratcan.app.entity.User;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.DiscountCodeNotFoundException;
import com.beratcan.app.repository.DiscountCodeRepository;
import com.beratcan.app.repository.DiscountCodeUsageRepository;
import com.beratcan.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * NOT: Bu metod purchase kayıtları başarıyla oluşturulduktan SONRA çağrılmalı
     */
    @Transactional
    public void useDiscountCode(Long discountCodeId, Long userId, String transactionId) {
        log.info("Using discount code: discountCodeId={}, userId={}, transactionId={}",
                discountCodeId, userId, transactionId);

        DiscountCode code = discountCodeRepository.findById(discountCodeId)
                .orElseThrow(() -> new DiscountCodeNotFoundException(discountCodeId));

        // Son limit kontrolü (race condition önleme)
        // İki kullanıcı aynı anda checkout yaparsa, bu kontrol limit aşımını önler
        if (code.getUsedCount() >= code.getUsageLimit()) {
            log.error("Discount code usage limit reached during use: codeId={}, usedCount={}, limit={}",
                    discountCodeId, code.getUsedCount(), code.getUsageLimit());
            throw new BadRequestException("Bu indirim kodu kullanım limitine ulaştı");
        }

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

}
