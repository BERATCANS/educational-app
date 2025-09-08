package com.beratcan.app.service;

import com.beratcan.app.dto.request.BookRequest;
import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.BookCode;
import com.beratcan.app.entity.Topic;
import com.beratcan.app.entity.User;
import com.beratcan.app.repository.BookCodeRepository;
import com.beratcan.app.repository.BookRepository;
import com.beratcan.app.repository.TopicRepository;
import com.beratcan.app.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final BookCodeRepository codeRepository;
    private final UserRepository userRepository;
    private final TopicRepository topicRepository;

    public BookService(BookRepository bookRepository, BookCodeRepository codeRepository, UserRepository userRepository, TopicRepository topicRepository) {
        this.bookRepository = bookRepository;
        this.codeRepository = codeRepository;
        this.userRepository = userRepository;
        this.topicRepository = topicRepository;
    }

    public Book addBook(Book book, List<String> topics) {

        // book’u önce kaydet
        Book savedBook = bookRepository.save(book);

        if (topics != null) {
            for (String topicName : topics) {
                Topic topic = new Topic();
                topic.setTitle(topicName);
                topic = topicRepository.save(topic);
                savedBook.getTopics().add(topic);
            }
            savedBook = bookRepository.save(savedBook);
        }

        return savedBook;
    }

    // Kullanıcı kod kullanımı
    @Transactional
    public String useCode(Long userId, String codeValue) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return "Kullanıcı bulunamadı";

        User user = userOpt.get();

        Optional<BookCode> codeOpt = codeRepository.findByCodeAndUsedFalse(codeValue);
        if (codeOpt.isEmpty()) return "Kod geçersiz veya zaten kullanıldı";

        BookCode code = codeOpt.get();
        code.setUsed(true);
        code.setUsedBy(user);
        codeRepository.save(code);

        // Kullanıcıya kitabı ekle
        user.getOwnedBooks().add(code.getBook());
        userRepository.save(user);

        return "Kod başarıyla kullanıldı, kitap erişimi açıldı: " + code.getBook().getTitle();
    }

    public List<Book> getBooksForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Paralı kitaplar (sahip olduğu)
        List<Book> paidBooks = user.getOwnedBooks().stream()
                .filter(Book::isPaid)
                .collect(Collectors.toList());

        // Ücretsiz kitaplar
        List<Book> freeBooks = bookRepository.findByPaidFalse();

        // Önce paralı sonra ücretsiz kitaplar
        paidBooks.addAll(freeBooks);

        return paidBooks;
    }

    // 📌 Kitap güncelleme
    public Book updateBook(Long bookId, BookRequest request) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        book.setTitle(request.getTitle());
        book.setPublisher(request.getPublisher());
        book.setCoverImageUrl(request.getCoverImageUrl());
        book.setPaid(request.isPaid());

        // Kod güncellemesi gerekiyorsa buraya eklenebilir
        return bookRepository.save(book);
    }

    // 📌 Kitap silme
    public void deleteBook(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new RuntimeException("Book not found");
        }
        bookRepository.deleteById(bookId);
    }
}

