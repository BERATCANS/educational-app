package com.beratcan.app.controller;

import com.beratcan.app.service.RateLimitingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/rate-limit")
public class RateLimitController {

    private final RateLimitingService rateLimitingService;

    public RateLimitController(RateLimitingService rateLimitingService) {
        this.rateLimitingService = rateLimitingService;
    }

    @GetMapping("/status/{clientIp}")
    public ResponseEntity<Map<String, Object>> getRateLimitStatus(@PathVariable String clientIp) {
        Map<String, Object> status = new HashMap<>();

        status.put("login", Map.of(
                "remaining", rateLimitingService.getRemainingTokens(clientIp, "login"),
                "limit", 5));

        status.put("register", Map.of(
                "remaining", rateLimitingService.getRemainingTokens(clientIp, "register"),
                "limit", 3));

        status.put("api", Map.of(
                "remaining", rateLimitingService.getRemainingTokens(clientIp, "api"),
                "limit", 100));

        return ResponseEntity.ok(status);
    }

    @PostMapping("/reset/{clientIp}")
    public ResponseEntity<Map<String, String>> resetRateLimit(@PathVariable String clientIp) {
        rateLimitingService.resetRateLimit(clientIp);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Rate limit reset successfully for IP: " + clientIp);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "Rate limiting service is running");
        response.put("timestamp", java.time.Instant.now().toString());

        return ResponseEntity.ok(response);
    }
}
