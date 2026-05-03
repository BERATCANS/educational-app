package com.beratcan.app.controller;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.dto.request.AddToCartRequest;
import com.beratcan.app.dto.response.CartResponse;
import com.beratcan.app.dto.response.PurchaseResponse;
import com.beratcan.app.exception.IdempotencyConflictException;
import com.beratcan.app.service.CartService;
import com.beratcan.app.service.PurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class CartController {

    private final CartService cartService;
    private final PurchaseService purchaseService;

    /**
     * Sepete kitap ekle
     * POST /api/cart/add
     */
    @PostMapping("/add")
    @RateLimit(requestsPerMinute = 30, keyType = "USER", message = "Too many cart operations. Please try again later.")
    public ResponseEntity<?> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        CartResponse response = cartService.addToCart(userId, request.getBookId());
        return ResponseEntity.ok(response);
    }

    /**
     * Sepetten kitap çıkar
     * DELETE /api/cart/items/{cartItemId}
     */
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<?> removeFromCart(
            @PathVariable Long cartItemId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        CartResponse response = cartService.removeFromCart(userId, cartItemId);
        return ResponseEntity.ok(response);
    }

    /**
     * Sepeti görüntüle
     * GET /api/cart
     */
    @GetMapping
    public ResponseEntity<CartResponse> getCart(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        CartResponse response = cartService.getCart(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Sepeti temizle
     * DELETE /api/cart/clear
     */
    @DeleteMapping("/clear")
    public ResponseEntity<?> clearCart(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        cartService.clearCart(userId);
        return ResponseEntity.ok(Map.of("message", "Sepet başarıyla temizlendi"));
    }

    /**
     * Sepetteki ürün sayısı
     * GET /api/cart/count
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> getCartItemCount(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        int count = cartService.getCartItemCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Sepeti satın al (tüm kitapları birden)
     * POST /api/cart/checkout
     *
     * Idempotency-Key header sent → safe to retry; the same key for the same user
     * returns the original response without creating a second Purchase.
     */
    @PostMapping("/checkout")
    @RateLimit(requestsPerMinute = 3, keyType = "USER", message = "Too many checkout attempts. Please try again later.")
    public ResponseEntity<?> checkout(
            Authentication authentication,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        Long userId = Long.parseLong(authentication.getName());
        PurchaseResponse purchase;
        try {
            purchase = purchaseService.checkoutCart(userId, idempotencyKey);
        } catch (IdempotencyConflictException conflict) {
            purchase = purchaseService.findByIdempotencyKey(userId, idempotencyKey)
                    .orElseThrow(() -> conflict);
        }
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.CREATED);
        if (idempotencyKey != null) {
            builder = builder.header("Idempotency-Key", idempotencyKey);
        }
        return builder.body(Map.of(
                "message", "Sepetteki tüm kitaplar başarıyla satın alındı",
                "purchase", purchase,
                "totalItems", purchase.getItemCount()));
    }

    /**
     * Sepetteki priceAtAdd değerlerini güncel fiyatlara çek.
     * POST /api/cart/refresh-prices
     *
     * Checkout 409 (PRICE_CHANGED) döndüğünde frontend kullanıcıya yeni toplamı
     * onaylatıp bu endpoint'i çağırır, sonra yeni Idempotency-Key ile checkout'u
     * tekrarlar.
     */
    @PostMapping("/refresh-prices")
    public ResponseEntity<CartResponse> refreshPrices(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(cartService.refreshPrices(userId));
    }

    @PostMapping("/apply-discount")
    @RateLimit(requestsPerMinute = 10, keyType = "USER", message = "Too many discount code attempts. Please try again later.")
    public ResponseEntity<CartResponse> applyDiscountCode(
            @RequestParam String code,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(cartService.applyDiscountCode(userId, code));
    }

    @DeleteMapping("/remove-discount")
    public ResponseEntity<CartResponse> removeDiscountCode(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(cartService.removeDiscountCode(userId));
    }
}
