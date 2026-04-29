package com.beratcan.app.controller;

import com.beratcan.app.dto.request.BookRequest;
import com.beratcan.app.dto.response.BookResponse;
import com.beratcan.app.service.BookAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookAdminController {

    private final BookAdminService bookAdminService;

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/add")
    public BookResponse addBook(@Valid @RequestBody BookRequest request) {
        return bookAdminService.addBook(request);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/{bookId}")
    public ResponseEntity<BookResponse> updateBook(
            @PathVariable Long bookId,
            @Valid @RequestBody BookRequest request) {
        BookResponse updatedBook = bookAdminService.updateBook(bookId, request);
        bookAdminService.countBookData(bookId);
        return ResponseEntity.ok(updatedBook);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @DeleteMapping("/{bookId}")
    public ResponseEntity<String> deleteBook(@PathVariable Long bookId) {
        bookAdminService.deleteBook(bookId);
        return ResponseEntity.ok("Book deleted successfully.");
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping(value = "/{bookId}/cover", consumes = "multipart/form-data")
    public ResponseEntity<BookResponse> uploadCover(
            @PathVariable Long bookId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(bookAdminService.uploadCover(bookId, file));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/{bookId}/count")
    public ResponseEntity<String> countBookData(@PathVariable Long bookId) {
        bookAdminService.countBookData(bookId);
        return ResponseEntity.ok("Book ID " + bookId + " sayıları başarıyla güncellendi.");
    }
}
