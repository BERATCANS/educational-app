package com.beratcan.app.controller;

import com.beratcan.app.dto.request.UpdateBookCodeRequest;
import com.beratcan.app.dto.response.BookCodeResponse;
import com.beratcan.app.service.BookCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/book-codes")
@RequiredArgsConstructor
public class BookCodeController {

    private final BookCodeService bookCodeService;

    /**
     * Tüm book code'ları getir
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<List<BookCodeResponse>> getAllBookCodes() {
        List<BookCodeResponse> codes = bookCodeService.getAllBookCodes();
        return ResponseEntity.ok(codes);
    }

    /**
     * Belirli bir kitaba ait tüm code'ları getir
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<BookCodeResponse>> getBookCodesByBookId(@PathVariable Long bookId) {
        List<BookCodeResponse> codes = bookCodeService.getBookCodesByBookId(bookId);
        return ResponseEntity.ok(codes);
    }

    /**
     * Book code güncelle
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/{codeId}")
    public ResponseEntity<BookCodeResponse> updateBookCode(
            @PathVariable Long codeId,
            @Valid @RequestBody UpdateBookCodeRequest request) {
        BookCodeResponse updated = bookCodeService.updateBookCode(codeId, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Book code sil
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @DeleteMapping("/{codeId}")
    public ResponseEntity<Map<String, String>> deleteBookCode(@PathVariable Long codeId) {
        bookCodeService.deleteBookCode(codeId);
        return ResponseEntity.ok(Map.of("message", "Book code başarıyla silindi."));
    }

    /**
     * Bir kitaba birden fazla kod ekle
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/book/{bookId}")
    public ResponseEntity<Map<String, String>> addCodes(
            @PathVariable Long bookId,
            @RequestBody List<String> codes) {
        String result = bookCodeService.addCodes(bookId, codes);
        return ResponseEntity.ok(Map.of("message", result));
    }
}
