package com.beratcan.app.controller;

import com.beratcan.app.dto.request.BookRequest;
import com.beratcan.app.dto.response.BookResponse;
import com.beratcan.app.dto.response.BookStoreResponse;
import com.beratcan.app.entity.Book;
import com.beratcan.app.service.BookService;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/add")
    public Book addBook(@RequestBody BookRequest request) {
        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setPublisher(request.getPublisher());
        book.setCoverImageUrl(request.getCoverImageUrl());
        book.setPaid(request.isPaid());
        book.setGradeLevel(request.getGradeLevel());
        book.setFieldType(request.getFieldType());
        book.setExamType(request.getExamType());
        book.setLessonType(request.getLessonType());
        book.setPrice(request.getPrice());

        // topics BookService'e paslanacak
        return bookService.addBook(book, request.getTopics());
    }

    @PostMapping("/use-code")
    public ResponseEntity<?> useCode(@RequestParam Long userId, @RequestParam String code) {
        try {
            String result = bookService.useCode(userId, code);
            return ResponseEntity.ok().body(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/add-codes")
    public ResponseEntity<?> addCodes(@RequestParam Long bookId, @RequestBody List<String> codes) {
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
    @PutMapping("/{bookId}")
    public ResponseEntity<Book> updateBook(
            @PathVariable Long bookId,
            @RequestBody BookRequest request) {
        Book updatedBook = bookService.updateBook(bookId, request);
        bookService.countBookData(bookId);
        return ResponseEntity.ok(updatedBook);
    }

    // 📌 DELETE - Kitap silme
    @DeleteMapping("/{bookId}")
    public ResponseEntity<String> deleteBook(@PathVariable Long bookId) {
        bookService.deleteBook(bookId);
        return ResponseEntity.ok("Book deleted successfully.");
    }

    // 📊 COUNT - Kitap verilerini say ve güncelle
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
    public List<BookStoreResponse> getBookStore() {
        return bookService.getBookStore();
    }
}
