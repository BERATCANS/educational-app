package com.beratcan.app.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rate Limiting Annotation
 * Endpoint-specific rate limiting için kullanılır
 * 
 * Örnek kullanım:
 * 
 * @RateLimit(requestsPerMinute = 5, keyType = "IP")
 *                              @PostMapping("/login")
 *                              public ResponseEntity<?> login(...) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Dakikada izin verilen maksimum request sayısı
     * Default: 60 requests per minute
     */
    int requestsPerMinute() default 60;

    /**
     * Key type: IP, USER, veya GLOBAL
     * - IP: IP adresi bazlı rate limiting (brute force koruması için)
     * - USER: Kullanıcı ID bazlı rate limiting (authenticated users için)
     * - GLOBAL: Tüm kullanıcılar için global limit
     * 
     * Default: IP
     */
    String keyType() default "IP";

    /**
     * Rate limit aşıldığında gösterilecek özel mesaj
     * Default: Generic mesaj kullanılır
     */
    String message() default "Rate limit exceeded. Please try again later.";
}
