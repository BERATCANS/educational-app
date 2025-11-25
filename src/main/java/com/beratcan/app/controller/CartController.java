package com.beratcan.app.controller;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.dto.request.AddToCartRequest;
import com.beratcan.app.dto.response.CartResponse;
import com.beratcan.app.dto.response.PurchaseResponse;
import com.beratcan.app.service.CartService;
import com.beratcan.app.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
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
            @RequestBody AddToCartRequest request,
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
     */
    @PostMapping("/checkout")
    @RateLimit(requestsPerMinute = 3, keyType = "USER", message = "Too many checkout attempts. Please try again later.")
    public ResponseEntity<?> checkout(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        List<PurchaseResponse> purchases = purchaseService.checkoutCart(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Sepetteki tüm kitaplar başarıyla satın alındı",
                "purchases", purchases,
                "totalItems", purchases.size()));
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
