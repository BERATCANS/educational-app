package com.beratcan.app.controller;

import com.beratcan.app.dto.BookRequestDTO;
import com.beratcan.app.entity.Book;
import com.beratcan.app.service.BookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping("/add")
    public Book addBook(@RequestBody BookRequestDTO request) {
        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setPublisher(request.getPublisher());
        book.setCoverImageUrl(request.getCoverImageUrl());
        book.setPaid(request.isPaid());

        // topics BookService'e paslanacak
        return bookService.addBook(book, request.getTopics());
    }

    @PostMapping("/use-code")
    public String useCode(@RequestParam Long userId, @RequestParam String code) {
        return bookService.useCode(userId, code);
    }

    @GetMapping("/for-user/{userId}")
    public List<Book> getBooksForUser(@PathVariable Long userId) {
        return bookService.getBooksForUser(userId);
    }

    // 📌 PUT - Kitap güncelleme
    @PutMapping("/{bookId}")
    public ResponseEntity<Book> updateBook(
            @PathVariable Long bookId,
            @RequestBody BookRequestDTO request) {
        Book updatedBook = bookService.updateBook(bookId, request);
        return ResponseEntity.ok(updatedBook);
    }

    // 📌 DELETE - Kitap silme
    @DeleteMapping("/{bookId}")
    public ResponseEntity<String> deleteBook(@PathVariable Long bookId) {
        bookService.deleteBook(bookId);
        return ResponseEntity.ok("Book deleted successfully.");
    }
}
