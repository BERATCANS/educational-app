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
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookAdminService {

    private final BookRepository bookRepository;
    private final TopicRepository topicRepository;
    private final TestRepository testRepository;
    private final PublisherRepository publisherRepository;

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
        List<Test> testsWithQuestions = topicIds.isEmpty()
                ? List.of()
                : testRepository.findByTopicIdsWithQuestions(topicIds);

        // 5. Test'leri topic ID'lerine göre grupla
        Map<Long, List<Test>> testsByTopicId = new java.util.HashMap<>();
        for (Test test : testsWithQuestions) {
            Topic topic = test.getTopic();
            if (topic != null) {
                Long topicId = topic.getId();
                testsByTopicId.computeIfAbsent(topicId, k -> new java.util.ArrayList<>()).add(test);
            } else {
                log.warn("Test'in topic'i null: testId={}", test.getId());
            }
        }

        // 6. Test ID'lerine göre questions sayısını map'le (performans için)
        Map<Long, Integer> questionCountByTestId = testsWithQuestions.stream()
                .collect(java.util.stream.Collectors.toMap(
                        Test::getId,
                        test -> test.getQuestions() != null ? test.getQuestions().size() : 0));

        int totalTopics = 0;
        int totalTests = 0;
        int totalQuestions = 0;

        // Her topic için sayıları hesapla ve güncelle
        for (Topic topic : topics) {
            totalTopics++;

            // testsWithQuestions listesinden bu topic'e ait test'leri al
            List<Test> topicTests = testsByTopicId.getOrDefault(topic.getId(), List.of());
            int topicTestCount = topicTests.size();
            int topicQuestionCount = 0;

            log.debug("Topic için sayılar hesaplanıyor: topicId={}, testCount={}", topic.getId(), topicTestCount);

            // Her test için sayıları hesapla ve güncelle
            for (Test test : topicTests) {
                totalTests++;

                // Questions sayısını map'ten al (fetch edilmiş test'lerden)
                int testQuestionCount = questionCountByTestId.getOrDefault(test.getId(), 0);
                topicQuestionCount += testQuestionCount;
                totalQuestions += testQuestionCount;

                // Test'in soru sayısını güncelle ve kaydet
                // Test'i repository'den yeniden yükle (detached entity sorununu önlemek için)
                Test testToUpdate = testRepository.findById(test.getId())
                        .orElseThrow(() -> new TestNotFoundException(test.getId()));
                int oldCount = testToUpdate.getNumberOfQuestions();
                testToUpdate.setNumberOfQuestions(testQuestionCount);
                testRepository.saveAndFlush(testToUpdate);
                log.info("Test güncellendi: testId={}, oldNumberOfQuestions={}, newNumberOfQuestions={}",
                        test.getId(), oldCount, testQuestionCount);
            }

            // Topic'in test ve soru sayılarını güncelle ve kaydet
            // Topic'i repository'den yeniden yükle (detached entity sorununu önlemek için)
            Topic topicToUpdate = topicRepository.findById(topic.getId())
                    .orElseThrow(() -> new TopicNotFoundException(topic.getId()));
            int oldTestCount = topicToUpdate.getNumberOfTests();
            int oldQuestionCount = topicToUpdate.getNumberOfQuestions();
            topicToUpdate.setNumberOfTests(topicTestCount);
            topicToUpdate.setNumberOfQuestions(topicQuestionCount);
            topicRepository.saveAndFlush(topicToUpdate);
            log.info(
                    "Topic güncellendi: topicId={}, oldNumberOfTests={}, newNumberOfTests={}, oldNumberOfQuestions={}, newNumberOfQuestions={}",
                    topic.getId(), oldTestCount, topicTestCount, oldQuestionCount, topicQuestionCount);
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
