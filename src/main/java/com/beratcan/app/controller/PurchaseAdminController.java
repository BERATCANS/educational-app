package com.beratcan.app.controller;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.constants.AppConstants;
import com.beratcan.app.dto.response.PurchaseResponse;
import com.beratcan.app.entity.enums.PurchaseStatus;
import com.beratcan.app.service.PurchaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/purchases")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class PurchaseAdminController {

    private final PurchaseAdminService purchaseAdminService;

    /**
     * Tüm satın almaları arama ve filtreleme ile sayfalayarak getirir
     * GET /api/admin/purchases?username=john&bookTitle=matematik&status=COMPLETED&page=0&size=20
     */
    @GetMapping
    @RateLimit(requestsPerMinute = 30, keyType = "USER", message = "Too many admin purchase search requests.")
    public ResponseEntity<Page<PurchaseResponse>> searchPurchases(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String bookTitle,
            @RequestParam(required = false) PurchaseStatus status,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + AppConstants.DEFAULT_PAGE_SIZE) int size) {

        Page<PurchaseResponse> purchases = purchaseAdminService.searchPurchases(
                username, bookTitle, status, transactionId, startDate, endDate, minPrice, maxPrice, page, size);

        return ResponseEntity.ok(purchases);
    }

    /**
     * Belirli bir satın almanın tüm detaylarını getirir
     * GET /api/admin/purchases/{id}
     */
    @GetMapping("/{id}")
    @RateLimit(requestsPerMinute = 30, keyType = "USER", message = "Too many admin purchase detail requests.")
    public ResponseEntity<PurchaseResponse> getPurchaseDetail(@PathVariable Long id) {
        PurchaseResponse response = purchaseAdminService.getPurchaseDetail(id);
        return ResponseEntity.ok(response);
    }
}
