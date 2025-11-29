package com.beratcan.app.controller;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.constants.AppConstants;
import com.beratcan.app.dto.request.DiscountCodeRequest;
import com.beratcan.app.dto.response.DiscountCodeResponse;
import com.beratcan.app.service.DiscountCodeAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/discount-codes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DiscountCodeAdminController {

    private final DiscountCodeAdminService discountCodeAdminService;

    /**
     * İndirim kodu oluştur
     * POST /api/admin/discount-codes
     */
    @PostMapping
    @RateLimit(requestsPerMinute = 10, keyType = "USER", message = "Too many discount code creation requests.")
    public ResponseEntity<DiscountCodeResponse> createDiscountCode(
            @Valid @RequestBody DiscountCodeRequest request) {
        DiscountCodeResponse response = discountCodeAdminService.createDiscountCode(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Tüm indirim kodlarını listele (pagination destekli)
     * GET /api/admin/discount-codes?page=0&size=20
     */
    @GetMapping
    @RateLimit(requestsPerMinute = 30, keyType = "USER", message = "Too many discount code list requests.")
    public ResponseEntity<Page<DiscountCodeResponse>> getAllDiscountCodes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + AppConstants.DEFAULT_PAGE_SIZE) int size) {
        Page<DiscountCodeResponse> discountCodes = discountCodeAdminService.getAllDiscountCodes(page, size);
        return ResponseEntity.ok(discountCodes);
    }

    /**
     * İndirim kodunu ID ile getir
     * GET /api/admin/discount-codes/{id}
     */
    @GetMapping("/{id}")
    @RateLimit(requestsPerMinute = 30, keyType = "USER", message = "Too many discount code detail requests.")
    public ResponseEntity<DiscountCodeResponse> getDiscountCodeById(@PathVariable Long id) {
        DiscountCodeResponse response = discountCodeAdminService.getDiscountCodeById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * İndirim kodunu güncelle
     * PUT /api/admin/discount-codes/{id}
     */
    @PutMapping("/{id}")
    @RateLimit(requestsPerMinute = 10, keyType = "USER", message = "Too many discount code update requests.")
    public ResponseEntity<DiscountCodeResponse> updateDiscountCode(
            @PathVariable Long id,
            @Valid @RequestBody DiscountCodeRequest request) {
        DiscountCodeResponse response = discountCodeAdminService.updateDiscountCode(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * İndirim kodunu sil
     * DELETE /api/admin/discount-codes/{id}
     */
    @DeleteMapping("/{id}")
    @RateLimit(requestsPerMinute = 5, keyType = "USER", message = "Too many discount code deletion requests.")
    public ResponseEntity<Void> deleteDiscountCode(@PathVariable Long id) {
        discountCodeAdminService.deleteDiscountCode(id);
        return ResponseEntity.noContent().build();
    }
}
