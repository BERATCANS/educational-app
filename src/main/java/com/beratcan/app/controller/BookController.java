package com.beratcan.app.controller;

import com.beratcan.app.dto.request.BookRequest;
import com.beratcan.app.dto.response.BookResponse;
import com.beratcan.app.dto.response.BookStoreResponse;
import com.beratcan.app.dto.response.PageResponse;
import com.beratcan.app.entity.Book;
import com.beratcan.app.service.BookService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public Book addBook(@RequestBody BookRequest request) {
        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setPublisher(request.getPublisher());
        book.setCoverImageUrl(request.getCoverImageUrl());
        book.setPaid(request.isPaid());
        book.setGradeLevel(request.getGradeLevel());
        book.setExamType(request.getExamType());
        book.setLessonType(request.getLessonType());
        book.setPrice(request.getPrice());

        // topics BookService'e paslanacak
        return bookService.addBook(book, request.getTopics());
    }

    @PostMapping("/use-code")
    public ResponseEntity<?> useCode(
            @RequestParam String code,
            Authentication authentication) {
        try {
            Long userId = Long.parseLong(authentication.getName());
            String result = bookService.useCode(userId, code);
            return ResponseEntity.ok().body(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add-codes")
    public ResponseEntity<?> addCodes(
            @RequestParam Long bookId,
            @RequestBody List<String> codes) {
        try {
            String result = bookService.addCodes(bookId, codes);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/for-user/{userId}")
    public List<BookResponse> getBooksForUser(@PathVariable Long userId) {
        return bookService.getBooksForUser(userId);
    }

    // 📌 PUT - Kitap güncelleme
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{bookId}")
    public ResponseEntity<Book> updateBook(
            @PathVariable Long bookId,
            @RequestBody BookRequest request) {
        Book updatedBook = bookService.updateBook(bookId, request);
        bookService.countBookData(bookId);
        return ResponseEntity.ok(updatedBook);
    }

    // 📌 DELETE - Kitap silme
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{bookId}")
    public ResponseEntity<String> deleteBook(@PathVariable Long bookId) {
        bookService.deleteBook(bookId);
        return ResponseEntity.ok("Book deleted successfully.");
    }

    // 📊 COUNT - Kitap verilerini say ve güncelle
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{bookId}/count")
    public ResponseEntity<String> countBookData(@PathVariable Long bookId) {
        try {
            bookService.countBookData(bookId);
            return ResponseEntity.ok("Book ID " + bookId + " sayıları başarıyla güncellendi.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Hata: " + e.getMessage());
        }
    }

    @GetMapping("/store")
    public PageResponse<BookStoreResponse> getBookStore(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                Long userId = Long.parseLong(authentication.getName());
                return bookService.getBookStore(userId, page, size);
            } catch (Exception e) {
                // Kullanıcı ID'si parse edilemezse, tüm kitapları dön
                return bookService.getBookStore(null, page, size);
            }
        }
        // Giriş yapmamış kullanıcılar için tüm kitapları dön
        return bookService.getBookStore(null, page, size);
    }
}
