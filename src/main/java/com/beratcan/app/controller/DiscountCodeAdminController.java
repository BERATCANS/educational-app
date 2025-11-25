package com.beratcan.app.controller;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.dto.request.DiscountCodeRequest;
import com.beratcan.app.dto.response.DiscountCodeResponse;
import com.beratcan.app.service.DiscountCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/discount-codes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DiscountCodeAdminController {

    private final DiscountCodeService discountCodeService;

    @PostMapping
    @RateLimit(requestsPerMinute = 10, keyType = "USER", message = "Too many discount code creation requests.")
    public ResponseEntity<DiscountCodeResponse> createDiscountCode(
            @RequestBody DiscountCodeRequest request) {
        DiscountCodeResponse response = discountCodeService.createDiscountCode(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // TODO: Diğer admin endpoint'leri (list, update, delete, etc.)
    // Şimdilik sadece create endpoint'i eklendi
}
