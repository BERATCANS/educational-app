package com.beratcan.app.service;

import com.beratcan.app.dto.request.DiscountCodeRequest;
import com.beratcan.app.dto.response.DiscountCodeResponse;
import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.DiscountCode;
import com.beratcan.app.entity.Publisher;
import com.beratcan.app.entity.enums.DiscountType;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.ConflictException;
import com.beratcan.app.exception.DiscountCodeNotFoundException;
import com.beratcan.app.repository.BookRepository;
import com.beratcan.app.repository.DiscountCodeRepository;
import com.beratcan.app.repository.PublisherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountCodeAdminService {

    private final DiscountCodeRepository discountCodeRepository;
    private final PublisherRepository publisherRepository;
    private final BookRepository bookRepository;

    // Validasyon sabitleri
    private static final BigDecimal MIN_PERCENTAGE = BigDecimal.ONE;
    private static final BigDecimal MAX_PERCENTAGE = new BigDecimal("100");
    private static final BigDecimal MIN_FIXED_AMOUNT = BigDecimal.ONE;

    /**
     * Admin: İndirim kodu oluştur
     */
    @Transactional
    public DiscountCodeResponse createDiscountCode(DiscountCodeRequest request) {
        log.info("Creating discount code: code={}", request.getCode());

        // Validasyonlar
        validateDiscountCodeRequest(request);

        // Kod zaten var mı?
        if (discountCodeRepository.existsByCode(request.getCode())) {
            throw new ConflictException("Bu indirim kodu zaten mevcut: " + request.getCode());
        }

        DiscountCode discountCode = new DiscountCode();
        discountCode.setCode(request.getCode().toUpperCase().trim());
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
     * Admin: Tüm indirim kodlarını listele (pagination destekli)
     */
    @Transactional(readOnly = true)
    public Page<DiscountCodeResponse> getAllDiscountCodes(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<DiscountCode> discountCodes = discountCodeRepository.findAll(pageable);
        return discountCodes.map(this::toDiscountCodeResponse);
    }

    /**
     * Admin: İndirim kodunu ID ile getir
     */
    @Transactional(readOnly = true)
    public DiscountCodeResponse getDiscountCodeById(Long id) {
        DiscountCode discountCode = discountCodeRepository.findById(id)
                .orElseThrow(() -> new DiscountCodeNotFoundException(id));
        return toDiscountCodeResponse(discountCode);
    }

    /**
     * Admin: İndirim kodunu güncelle
     */
    @Transactional
    public DiscountCodeResponse updateDiscountCode(Long id, DiscountCodeRequest request) {
        log.info("Updating discount code: id={}, code={}", id, request.getCode());

        DiscountCode discountCode = discountCodeRepository.findById(id)
                .orElseThrow(() -> new DiscountCodeNotFoundException(id));

        // Validasyonlar
        validateDiscountCodeRequest(request);

        // Kod değişiyorsa, yeni kod benzersiz mi kontrol et
        if (request.getCode() != null && !request.getCode().equalsIgnoreCase(discountCode.getCode())) {
            if (discountCodeRepository.existsByCode(request.getCode())) {
                throw new ConflictException("Bu indirim kodu zaten mevcut: " + request.getCode());
            }
            discountCode.setCode(request.getCode().toUpperCase().trim());
        }

        // Diğer alanları güncelle
        if (request.getDescription() != null) {
            discountCode.setDescription(request.getDescription());
        }
        if (request.getDiscountType() != null) {
            discountCode.setDiscountType(request.getDiscountType());
        }
        if (request.getDiscountValue() != null) {
            discountCode.setDiscountValue(request.getDiscountValue());
        }
        if (request.getUsageLimit() != null) {
            discountCode.setUsageLimit(request.getUsageLimit());
        }
        if (request.getMinCartTotal() != null) {
            discountCode.setMinCartTotal(request.getMinCartTotal());
        }
        if (request.getMaxCartTotal() != null) {
            discountCode.setMaxCartTotal(request.getMaxCartTotal());
        }
        if (request.getValidFrom() != null) {
            discountCode.setValidFrom(request.getValidFrom());
        }
        if (request.getValidUntil() != null) {
            discountCode.setValidUntil(request.getValidUntil());
        }
        if (request.getIsActive() != null) {
            discountCode.setIsActive(request.getIsActive());
        }
        if (request.getMaxUsagePerUser() != null) {
            discountCode.setMaxUsagePerUser(request.getMaxUsagePerUser());
        }

        // Yayınevi güncelle
        if (request.getApplicablePublisherId() != null) {
            Publisher publisher = publisherRepository.findById(request.getApplicablePublisherId())
                    .orElseThrow(() -> new com.beratcan.app.exception.PublisherNotFoundException(
                            request.getApplicablePublisherId()));
            discountCode.setApplicablePublisher(publisher);
        } else if (request.getApplicablePublisherId() == null && discountCode.getApplicablePublisher() != null) {
            // null gönderilirse kaldır
            discountCode.setApplicablePublisher(null);
        }

        // Kitaplar güncelle
        if (request.getApplicableBookIds() != null) {
            if (request.getApplicableBookIds().isEmpty()) {
                discountCode.getApplicableBooks().clear();
            } else {
                Set<Book> books = request.getApplicableBookIds().stream()
                        .map(bookId -> bookRepository.findById(bookId)
                                .orElseThrow(() -> new com.beratcan.app.exception.BookNotFoundException(bookId)))
                        .collect(Collectors.toSet());
                discountCode.setApplicableBooks(books);
            }
        }

        discountCode = discountCodeRepository.save(discountCode);
        log.info("Discount code updated successfully: id={}, code={}", discountCode.getId(), discountCode.getCode());

        return toDiscountCodeResponse(discountCode);
    }

    /**
     * Admin: İndirim kodunu sil
     */
    @Transactional
    public void deleteDiscountCode(Long id) {
        log.info("Deleting discount code: id={}", id);

        DiscountCode discountCode = discountCodeRepository.findById(id)
                .orElseThrow(() -> new DiscountCodeNotFoundException(id));

        // Kullanım kayıtları varsa silme (cascade ile otomatik silinir)
        discountCodeRepository.delete(discountCode);
        log.info("Discount code deleted successfully: id={}, code={}", id, discountCode.getCode());
    }

    /**
     * İndirim kodu request validasyonu
     */
    private void validateDiscountCodeRequest(DiscountCodeRequest request) {
        if (request == null) {
            throw new BadRequestException("İndirim kodu bilgileri boş olamaz");
        }

        // Kod validasyonu
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw new BadRequestException("İndirim kodu boş olamaz");
        }
        if (request.getCode().length() > 50) {
            throw new BadRequestException("İndirim kodu en fazla 50 karakter olabilir");
        }

        // Açıklama validasyonu
        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            throw new BadRequestException("Açıklama boş olamaz");
        }
        if (request.getDescription().length() > 500) {
            throw new BadRequestException("Açıklama en fazla 500 karakter olabilir");
        }

        // Discount type validasyonu
        if (request.getDiscountType() == null) {
            throw new BadRequestException("İndirim tipi seçilmelidir (PERCENTAGE veya FIXED_AMOUNT)");
        }

        // Discount value validasyonu
        if (request.getDiscountValue() == null) {
            throw new BadRequestException("İndirim değeri boş olamaz");
        }

        if (request.getDiscountType() == DiscountType.PERCENTAGE) {
            // Percentage: 1-100 arası olmalı
            if (request.getDiscountValue().compareTo(MIN_PERCENTAGE) < 0 ||
                    request.getDiscountValue().compareTo(MAX_PERCENTAGE) > 0) {
                throw new BadRequestException("Yüzde indirim değeri 1 ile 100 arasında olmalıdır");
            }
        } else if (request.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            // Fixed amount: pozitif olmalı
            if (request.getDiscountValue().compareTo(MIN_FIXED_AMOUNT) < 0) {
                throw new BadRequestException("Sabit tutar indirimi en az 1 TL olmalıdır");
            }
        }

        // Usage limit validasyonu
        if (request.getUsageLimit() == null || request.getUsageLimit() <= 0) {
            throw new BadRequestException("Kullanım limiti en az 1 olmalıdır");
        }

        // Tarih validasyonu
        if (request.getValidFrom() == null) {
            throw new BadRequestException("Geçerlilik başlangıç tarihi boş olamaz");
        }
        if (request.getValidUntil() == null) {
            throw new BadRequestException("Geçerlilik bitiş tarihi boş olamaz");
        }
        if (request.getValidUntil().isBefore(request.getValidFrom())) {
            throw new BadRequestException("Geçerlilik bitiş tarihi, başlangıç tarihinden önce olamaz");
        }
        if (request.getValidUntil().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Geçerlilik bitiş tarihi geçmiş bir tarih olamaz");
        }

        // Min-max cart total validasyonu
        if (request.getMinCartTotal() != null && request.getMinCartTotal().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Minimum sepet tutarı negatif olamaz");
        }
        if (request.getMaxCartTotal() != null && request.getMaxCartTotal().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Maksimum sepet tutarı negatif olamaz");
        }
        if (request.getMinCartTotal() != null && request.getMaxCartTotal() != null) {
            if (request.getMinCartTotal().compareTo(request.getMaxCartTotal()) > 0) {
                throw new BadRequestException("Minimum sepet tutarı, maksimum sepet tutarından büyük olamaz");
            }
        }

        // Max usage per user validasyonu
        if (request.getMaxUsagePerUser() != null && request.getMaxUsagePerUser() <= 0) {
            throw new BadRequestException("Kullanıcı başına maksimum kullanım sayısı en az 1 olmalıdır");
        }
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
