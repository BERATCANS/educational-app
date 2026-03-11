package com.beratcan.app.service;

import com.beratcan.app.dto.response.PurchaseItemResponse;
import com.beratcan.app.dto.response.PurchaseResponse;
import com.beratcan.app.entity.Purchase;
import com.beratcan.app.entity.PurchaseItem;
import com.beratcan.app.entity.enums.PurchaseStatus;
import com.beratcan.app.exception.PurchaseNotFoundException;
import com.beratcan.app.repository.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PurchaseAdminService {

    private final PurchaseRepository purchaseRepository;

    /**
     * Admin: Tüm satın almaları dinamik filtrelerle sayfalı olarak getir
     */
    @Transactional(readOnly = true)
    public Page<PurchaseResponse> searchPurchases(
            Long userId,
            Long bookId,
            PurchaseStatus status,
            String transactionId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size) {

        log.info("Admin search purchases with filters: userId={}, bookId={}, status={}, transactionId={}, dates=[{} - {}], prices=[{} - {}]",
                userId, bookId, status, transactionId, startDate, endDate, minPrice, maxPrice);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "purchaseDate"));

        Page<Purchase> purchases = purchaseRepository.searchPurchases(
                userId, bookId, status, transactionId, startDate, endDate, minPrice, maxPrice, pageable);

        return purchases.map(this::mapToResponse);
    }

    /**
     * Admin: Belirli bir satın almanın tüm detaylarını getir
     */
    @Transactional(readOnly = true)
    public PurchaseResponse getPurchaseDetail(Long purchaseId) {
        log.info("Admin fetch purchase detail: purchaseId={}", purchaseId);

        Purchase purchase = purchaseRepository.findByIdWithUserAndBook(purchaseId)
                .orElseThrow(() -> {
                    log.error("Purchase not found: purchaseId={}", purchaseId);
                    return new PurchaseNotFoundException(purchaseId);
                });

        return mapToResponse(purchase);
    }

    // Purchase entity'sini PurchaseResponse'a dönüştür (PurchaseService ile aynı yapı)
    private PurchaseResponse mapToResponse(Purchase purchase) {
        PurchaseResponse response = new PurchaseResponse();
        response.setId(purchase.getId());
        response.setUserId(purchase.getUser().getId());
        response.setUserName(purchase.getUser().getFirstName() + " " + purchase.getUser().getLastName());
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

    // PurchaseItem entity'sini PurchaseItemResponse'a dönüştür
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
}
