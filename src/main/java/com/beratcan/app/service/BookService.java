package com.beratcan.app.service;

import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.BookCode;
import com.beratcan.app.entity.User;
import com.beratcan.app.repository.BookCodeRepository;
import com.beratcan.app.repository.BookRepository;
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

    public BookService(BookRepository bookRepository, BookCodeRepository codeRepository, UserRepository userRepository) {
        this.bookRepository = bookRepository;
        this.codeRepository = codeRepository;
        this.userRepository = userRepository;
    }

    // Kitap ekleme
    public Book addBook(Book book, List<String> codes) {
        Book savedBook = bookRepository.save(book);
        if (book.isPaid() && codes != null) {
            for (String codeValue : codes) {
                BookCode code = new BookCode();
                code.setBook(savedBook);
                code.setCode(codeValue);
                code.setUsed(false);
                codeRepository.save(code);
            }
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
}

