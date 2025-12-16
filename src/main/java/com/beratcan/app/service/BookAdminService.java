package com.beratcan.app.service;

import com.beratcan.app.dto.request.BookRequest;
import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.Test;
import com.beratcan.app.entity.Topic;
import com.beratcan.app.entity.Publisher;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.BookNotFoundException;
import com.beratcan.app.exception.PublisherNotFoundException;
import com.beratcan.app.repository.BookRepository;
import com.beratcan.app.repository.TestRepository;
import com.beratcan.app.repository.TopicRepository;
import com.beratcan.app.repository.PublisherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookAdminService {

    private final BookRepository bookRepository;
    private final TopicRepository topicRepository;
    private final TestRepository testRepository;
    private final PublisherRepository publisherRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @Transactional
    public Book addBook(BookRequest request) {
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
        // Book data sayılarını asenkron olarak hesapla (ana işlemi bloklamaz)
        // ApplicationContext üzerinden çağır (self-invocation sorununu çözmek için)
        BookAdminService self = applicationContext.getBean(BookAdminService.class);
        self.countBookDataAsync(savedBook.getId());
        return savedBook;
    }

    @Transactional
    public Book updateBook(Long bookId, BookRequest request) {
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

        return bookRepository.save(book);
    }

    @Transactional
    public void deleteBook(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new BookNotFoundException(bookId);
        }
        bookRepository.deleteById(bookId);
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
     */
    @Transactional
    public void countBookData(Long bookId) {
        // 1. Book'u topics ile birlikte getir (sadece bir bag fetch)
        Book book = bookRepository.findByIdWithTopics(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        // 2. Topics'ları tests ile birlikte getir (sadece bir bag fetch)
        List<Topic> topics = topicRepository.findByBookIdWithTests(bookId);

        // 3. Topic ID'lerini topla
        List<Long> topicIds = topics.stream()
                .map(Topic::getId)
                .toList();

        // 4. Tests'leri questions ile birlikte getir (sadece bir bag fetch)
        List<Test> tests = topicIds.isEmpty()
                ? List.of()
                : testRepository.findByTopicIdsWithQuestions(topicIds);

        // 5. Test'leri topic'lere göre grupla (performans için)
        Map<Long, List<Test>> testsByTopicId = tests.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        test -> test.getTopic().getId()));

        int totalTopics = 0;
        int totalTests = 0;
        int totalQuestions = 0;

        // Her topic için sayıları hesapla ve güncelle
        for (Topic topic : topics) {
            totalTopics++;

            List<Test> topicTests = testsByTopicId.getOrDefault(topic.getId(), List.of());
            int topicTestCount = topicTests.size();
            int topicQuestionCount = 0;

            // Her test için sayıları hesapla ve güncelle
            for (Test test : topicTests) {
                totalTests++;

                int testQuestionCount = test.getQuestions() != null ? test.getQuestions().size() : 0;
                topicQuestionCount += testQuestionCount;
                totalQuestions += testQuestionCount;

                // Test'in soru sayısını güncelle ve kaydet
                test.setNumberOfQuestions(testQuestionCount);
                testRepository.save(test);
            }

            // Topic'in test ve soru sayılarını güncelle ve kaydet
            topic.setNumberOfTests(topicTestCount);
            topic.setNumberOfQuestions(topicQuestionCount);
            topicRepository.save(topic);
        }

        // Book'un toplam sayılarını güncelle ve kaydet
        book.setNumberOfTopics(totalTopics);
        book.setNumberOfTests(totalTests);
        book.setNumberOfQuestions(totalQuestions);
        bookRepository.save(book);
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
}
