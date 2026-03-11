package com.beratcan.app.service;

import com.beratcan.app.dto.request.DiscountCodeRequest;
import com.beratcan.app.dto.response.DiscountCodeResponse;
import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.DiscountCode;
import com.beratcan.app.entity.Publisher;
import com.beratcan.app.entity.User;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.ConflictException;
import com.beratcan.app.exception.DiscountCodeNotFoundException;
import com.beratcan.app.repository.BookRepository;
import com.beratcan.app.repository.DiscountCodeRepository;
import com.beratcan.app.repository.PublisherRepository;
import com.beratcan.app.repository.UserRepository;
import com.beratcan.app.exception.PublisherNotFoundException;
import com.beratcan.app.exception.BookNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountCodeAdminService {

    private final DiscountCodeRepository discountCodeRepository;
    private final PublisherRepository publisherRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

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

        // Yayınevleri
        if (request.getApplicablePublisherIds() != null && !request.getApplicablePublisherIds().isEmpty()) {
            Set<Publisher> publishers = request.getApplicablePublisherIds().stream()
                    .map(publisherId -> publisherRepository.findById(publisherId)
                            .orElseThrow(() -> new PublisherNotFoundException(publisherId)))
                    .collect(Collectors.toSet());
            discountCode.setApplicablePublishers(publishers);
        }

        // Kitaplar
        if (request.getApplicableBookIds() != null && !request.getApplicableBookIds().isEmpty()) {
            Set<Book> books = request.getApplicableBookIds().stream()
                    .map(bookId -> bookRepository.findById(bookId)
                            .orElseThrow(() -> new BookNotFoundException(bookId)))
                    .collect(Collectors.toSet());
            discountCode.setApplicableBooks(books);
        }

        // Kullanıcılar (username ile)
        if (request.getApplicableUsernames() != null && !request.getApplicableUsernames().isEmpty()) {
            Set<User> users = request.getApplicableUsernames().stream()
                    .map(username -> userRepository.findByUsername(username)
                            .orElseThrow(() -> new BadRequestException(
                                    "Kullanıcı adı bulunamadı: " + username)))
                    .collect(Collectors.toSet());
            discountCode.setApplicableUsers(users);
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

        // Yayınevleri güncelle
        if (request.getApplicablePublisherIds() != null) {
            if (request.getApplicablePublisherIds().isEmpty()) {
                discountCode.getApplicablePublishers().clear();
            } else {
                Set<Publisher> publishers = request.getApplicablePublisherIds().stream()
                        .map(publisherId -> publisherRepository.findById(publisherId)
                                .orElseThrow(() -> new PublisherNotFoundException(publisherId)))
                        .collect(Collectors.toSet());
                discountCode.setApplicablePublishers(publishers);
            }
        }

        // Kitaplar güncelle
        if (request.getApplicableBookIds() != null) {
            if (request.getApplicableBookIds().isEmpty()) {
                discountCode.getApplicableBooks().clear();
            } else {
                Set<Book> books = request.getApplicableBookIds().stream()
                        .map(bookId -> bookRepository.findById(bookId)
                                .orElseThrow(() -> new BookNotFoundException(bookId)))
                        .collect(Collectors.toSet());
                discountCode.setApplicableBooks(books);
            }
        }

        // Kullanıcılar güncelle (username ile)
        if (request.getApplicableUsernames() != null) {
            if (request.getApplicableUsernames().isEmpty()) {
                discountCode.getApplicableUsers().clear();
            } else {
                Set<User> users = request.getApplicableUsernames().stream()
                        .map(username -> userRepository.findByUsername(username)
                                .orElseThrow(() -> new BadRequestException(
                                        "Kullanıcı adı bulunamadı: " + username)))
                        .collect(Collectors.toSet());
                discountCode.setApplicableUsers(users);
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
     * Not: Temel validasyonlar DTO'da @Valid ve custom validator ile yapılıyor.
     * Burada sadece business logic kontrolleri yapılıyor.
     */
    private void validateDiscountCodeRequest(DiscountCodeRequest request) {
        if (request == null) {
            throw new BadRequestException("İndirim kodu bilgileri boş olamaz");
        }
        // Diğer validasyonlar DTO'da @Valid annotation ve ValidDiscountCode custom
        // validator ile yapılıyor
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

        // Yayınevleri
        if (code.getApplicablePublishers() != null && !code.getApplicablePublishers().isEmpty()) {
            List<DiscountCodeResponse.PublisherSummary> publishers = code.getApplicablePublishers().stream()
                    .map(p -> new DiscountCodeResponse.PublisherSummary(p.getId(), p.getName()))
                    .collect(Collectors.toList());
            response.setApplicablePublishers(publishers);
        }

        // Kitaplar
        if (code.getApplicableBooks() != null && !code.getApplicableBooks().isEmpty()) {
            List<DiscountCodeResponse.BookSummary> books = code.getApplicableBooks().stream()
                    .map(b -> new DiscountCodeResponse.BookSummary(b.getId(), b.getTitle()))
                    .collect(Collectors.toList());
            response.setApplicableBooks(books);
        }

        // Kullanıcılar
        if (code.getApplicableUsers() != null && !code.getApplicableUsers().isEmpty()) {
            List<String> usernames = code.getApplicableUsers().stream()
                    .map(User::getUsername)
                    .collect(Collectors.toList());
            response.setApplicableUsernames(usernames);
        }

        return response;
    }
}
