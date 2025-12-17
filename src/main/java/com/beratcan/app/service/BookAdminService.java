package com.beratcan.app.service;

import com.beratcan.app.dto.request.BookRequest;
import com.beratcan.app.dto.response.BookResponse;
import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.Test;
import com.beratcan.app.entity.Topic;
import com.beratcan.app.entity.Publisher;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.BookNotFoundException;
import com.beratcan.app.exception.PublisherNotFoundException;
import com.beratcan.app.exception.TestNotFoundException;
import com.beratcan.app.exception.TopicNotFoundException;
import com.beratcan.app.repository.BookRepository;
import com.beratcan.app.repository.TestRepository;
import com.beratcan.app.repository.TopicRepository;
import com.beratcan.app.repository.PublisherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookAdminService {

    private final BookRepository bookRepository;
    private final TopicRepository topicRepository;
    private final TestRepository testRepository;
    private final PublisherRepository publisherRepository;
    private final com.beratcan.app.repository.QuestionRepository questionRepository;

    @Transactional
    public BookResponse addBook(BookRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("BookRequest cannot be null");
        }

        Book book = new Book();
        book.setTitle(request.getTitle());
        Publisher publisher = resolvePublisher(request);
        book.setPublisher(publisher);
        book.setCoverImageUrl(request.getCoverImageUrl());
        book.setPaid(request.isPaid());
        book.setGradeLevel(request.getGradeLevel());
        book.setExamType(request.getExamType());
        book.setLessonType(request.getLessonType());
        book.setDescription(request.getDescription());

        // paid false ise price'ı otomatik 0 yap
        if (!request.isPaid()) {
            book.setPrice(BigDecimal.ZERO);
        } else {
            book.setPrice(request.getPrice());
        }

        Book savedBook = bookRepository.save(book);
        return toBookResponse(savedBook);
    }

    @Transactional
    public BookResponse updateBook(Long bookId, BookRequest request) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        book.setTitle(request.getTitle());
        Publisher publisher = resolvePublisher(request);
        book.setPublisher(publisher);
        book.setCoverImageUrl(request.getCoverImageUrl());
        book.setPaid(request.isPaid());
        book.setGradeLevel(request.getGradeLevel());
        book.setExamType(request.getExamType());
        book.setLessonType(request.getLessonType());
        book.setDescription(request.getDescription());

        // paid false ise price'ı otomatik 0 yap (addBook ile tutarlılık için)
        if (!request.isPaid()) {
            book.setPrice(BigDecimal.ZERO);
        } else {
            book.setPrice(request.getPrice());
        }

        Book updatedBook = bookRepository.save(book);
        return toBookResponse(updatedBook);
    }

    @Transactional
    public void deleteBook(Long bookId) {
        // Entity'yi yükle ki cascade delete çalışsın
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        // Entity'yi yükledik, şimdi cascade delete çalışacak
        bookRepository.delete(book);
    }

    /**
     * Book data sayılarını asenkron olarak hesapla
     * Ana işlem thread'ini bloklamaz
     */
    @Async
    public void countBookDataAsync(Long bookId) {
        log.debug("countBookDataAsync başlatıldı: bookId={}", bookId);
        try {
            countBookData(bookId);
            log.debug("countBookDataAsync tamamlandı: bookId={}", bookId);
        } catch (Exception e) {
            log.error("countBookDataAsync hatası: bookId={}, error={}", bookId, e.getMessage(), e);
        }
    }

    /**
     * Book data sayılarını hesapla ve güncelle (transaction içinde)
     * Asenkron wrapper metod tarafından çağrılır
     * MultipleBagFetchException'ı önlemek için collections ayrı ayrı fetch edilir
     * REQUIRES_NEW: Her zaman yeni bir transaction başlatır, async metodun
     * transaction'ından bağımsızdır
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void countBookData(Long bookId) {
        log.info("countBookData başlatıldı: bookId={}", bookId);

        // 1. Kitabın var olduğunu kontrol et
        if (!bookRepository.existsById(bookId)) {
            throw new BookNotFoundException(bookId);
        }

        // 2. Bu kitaba ait tüm topic'leri al
        List<Topic> topics = topicRepository.findByBookId(bookId);

        int totalTests = 0;
        int totalQuestions = 0;

        // 3. Her topic için test ve soru sayılarını database'den direkt say ve güncelle
        for (Topic topic : topics) {
            // Topic'e ait test sayısını database'den al
            int testCount = testRepository.countByTopicId(topic.getId());

            // Topic'e ait soru sayısını database'den al
            int questionCount = questionRepository.countByTopicId(topic.getId());

            totalTests += testCount;
            totalQuestions += questionCount;

            // Topic'i güncelle
            Topic topicToUpdate = topicRepository.findById(topic.getId())
                    .orElseThrow(() -> new TopicNotFoundException(topic.getId()));
            topicToUpdate.setNumberOfTests(testCount);
            topicToUpdate.setNumberOfQuestions(questionCount);
            topicRepository.saveAndFlush(topicToUpdate);

            log.info("Topic güncellendi: topicId={}, numberOfTests={}, numberOfQuestions={}",
                    topic.getId(), testCount, questionCount);
        }

        // 4. Her test için soru sayısını güncelle
        for (Topic topic : topics) {
            List<Test> tests = testRepository.findByTopicId(topic.getId());
            for (Test test : tests) {
                int questionCount = questionRepository.countByTestId(test.getId());

                Test testToUpdate = testRepository.findById(test.getId())
                        .orElseThrow(() -> new TestNotFoundException(test.getId()));
                testToUpdate.setNumberOfQuestions(questionCount);
                testRepository.saveAndFlush(testToUpdate);

                log.info("Test güncellendi: testId={}, numberOfQuestions={}", test.getId(), questionCount);
            }
        }

        // 5. Book'u güncelle
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));
        book.setNumberOfTopics(topics.size());
        book.setNumberOfTests(totalTests);
        book.setNumberOfQuestions(totalQuestions);
        bookRepository.saveAndFlush(book);

        log.info("Book güncellendi: bookId={}, numberOfTopics={}, numberOfTests={}, numberOfQuestions={}",
                bookId, topics.size(), totalTests, totalQuestions);
    }

    private Publisher resolvePublisher(BookRequest request) {
        if (request.getPublisherId() != null) {
            return publisherRepository.findById(request.getPublisherId())
                    .orElseThrow(() -> new PublisherNotFoundException(request.getPublisherId()));
        }
        if (StringUtils.hasText(request.getPublisher())) {
            String normalizedName = request.getPublisher().trim();
            return publisherRepository.findByNameIgnoreCase(normalizedName)
                    .orElseGet(() -> {
                        Publisher newPublisher = new Publisher();
                        newPublisher.setName(normalizedName);
                        return publisherRepository.save(newPublisher);
                    });
        }
        throw new BadRequestException("Yayınevi bilgisi zorunludur");
    }

    /**
     * Book entity'sini BookResponse DTO'suna dönüştür
     */
    private BookResponse toBookResponse(Book book) {
        BookResponse response = new BookResponse();
        response.setId(book.getId());
        response.setTitle(book.getTitle());
        response.setPublisher(book.getPublisher() != null ? book.getPublisher().getName() : null);
        // Database'de filename var, response'da URL oluştur
        // Eğer zaten URL formatındaysa (eski kayıtlar için) olduğu gibi bırak
        String coverImageUrl = book.getCoverImageUrl();
        if (coverImageUrl != null && !coverImageUrl.isEmpty() && !coverImageUrl.startsWith("api/files/")) {
            response.setCoverImageUrl("api/files/" + coverImageUrl);
        } else {
            response.setCoverImageUrl(coverImageUrl);
        }
        response.setPaid(book.isPaid());
        // Hesaplanmış alanları entity'deki getter metodlarından al
        response.setNumberOfTopics(book.getNumberOfTopics());
        response.setNumberOfTests(book.getNumberOfTests());
        response.setNumberOfQuestions(book.getNumberOfQuestions());
        return response;
    }
}
