package com.beratcan.app.controller;

import com.beratcan.app.dto.request.BookRequest;
import com.beratcan.app.dto.response.BookResponse;
import com.beratcan.app.service.BookAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookAdminController {

    private final BookAdminService bookAdminService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public BookResponse addBook(@Valid @RequestBody BookRequest request) {
        return bookAdminService.addBook(request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{bookId}")
    public ResponseEntity<BookResponse> updateBook(
            @PathVariable Long bookId,
            @Valid @RequestBody BookRequest request) {
        BookResponse updatedBook = bookAdminService.updateBook(bookId, request);
        bookAdminService.countBookData(bookId);
        return ResponseEntity.ok(updatedBook);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{bookId}")
    public ResponseEntity<String> deleteBook(@PathVariable Long bookId) {
        bookAdminService.deleteBook(bookId);
        return ResponseEntity.ok("Book deleted successfully.");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{bookId}/count")
    public ResponseEntity<String> countBookData(@PathVariable Long bookId) {
        bookAdminService.countBookData(bookId);
        return ResponseEntity.ok("Book ID " + bookId + " sayıları başarıyla güncellendi.");
    }
}
