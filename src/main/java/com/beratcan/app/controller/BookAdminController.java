package com.beratcan.app.controller;

import com.beratcan.app.dto.request.BookRequest;
import com.beratcan.app.entity.Book;
import com.beratcan.app.service.BookCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookAdminController {

    private final BookCatalogService bookCatalogService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public Book addBook(@RequestBody BookRequest request) {
        return bookCatalogService.addBook(request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{bookId}")
    public ResponseEntity<Book> updateBook(
            @PathVariable Long bookId,
            @RequestBody BookRequest request) {
        Book updatedBook = bookCatalogService.updateBook(bookId, request);
        bookCatalogService.countBookData(bookId);
        return ResponseEntity.ok(updatedBook);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{bookId}")
    public ResponseEntity<String> deleteBook(@PathVariable Long bookId) {
        bookCatalogService.deleteBook(bookId);
        return ResponseEntity.ok("Book deleted successfully.");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{bookId}/count")
    public ResponseEntity<String> countBookData(@PathVariable Long bookId) {
        try {
            bookCatalogService.countBookData(bookId);
            return ResponseEntity.ok("Book ID " + bookId + " sayıları başarıyla güncellendi.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Hata: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add-codes")
    public ResponseEntity<?> addCodes(
            @RequestParam Long bookId,
            @RequestBody List<String> codes) {
        try {
            String result = bookCatalogService.addCodes(bookId, codes);
            return ResponseEntity.ok(java.util.Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
}

