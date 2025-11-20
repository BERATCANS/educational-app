package com.beratcan.app.controller;

import com.beratcan.app.dto.request.PurchaseRequest;
import com.beratcan.app.dto.response.PurchaseResponse;
import com.beratcan.app.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    /**
     * Kitap satın al
     * POST /api/purchases/buy/{bookId}
     */
    @PostMapping("/buy/{bookId}")
    public ResponseEntity<?> buyBook(
            @PathVariable Long bookId,
            @RequestBody(required = false) PurchaseRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());

        if (request == null) {
            request = new PurchaseRequest();
        }

        PurchaseResponse response = purchaseService.buyBook(userId, bookId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Kullanıcının tüm satın alma geçmişini getir
     * GET /api/purchases/my-purchases
     */
    @GetMapping("/my-purchases")
    public ResponseEntity<List<PurchaseResponse>> getMyPurchases(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        List<PurchaseResponse> purchases = purchaseService.getUserPurchases(userId);
        return ResponseEntity.ok(purchases);
    }

    /**
     * Kullanıcının tamamlanmış satın almalarını getir
     * GET /api/purchases/completed
     */
    @GetMapping("/completed")
    public ResponseEntity<List<PurchaseResponse>> getCompletedPurchases(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        List<PurchaseResponse> purchases = purchaseService.getUserCompletedPurchases(userId);
        return ResponseEntity.ok(purchases);
    }

    /**
     * Belirli bir satın alma kaydını getir
     * GET /api/purchases/{purchaseId}
     */
    @GetMapping("/{purchaseId}")
    public ResponseEntity<?> getPurchaseById(
            @PathVariable Long purchaseId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        PurchaseResponse response = purchaseService.getPurchaseById(purchaseId, userId);
        return ResponseEntity.ok(response);
    }
}
