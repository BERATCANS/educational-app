package com.beratcan.app.service;

import com.beratcan.app.dto.request.TestRequest;
import com.beratcan.app.dto.response.TestResponse;
import com.beratcan.app.entity.Test;
import com.beratcan.app.entity.Topic;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.ConflictException;
import com.beratcan.app.exception.TestNotFoundException;
import com.beratcan.app.exception.TopicNotFoundException;
import com.beratcan.app.repository.TestRepository;
import com.beratcan.app.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestService {

    private final TestRepository testRepository;
    private final TopicRepository topicRepository;

    @Autowired
    private ApplicationContext applicationContext;

    // CREATE
    @Transactional
    public TestResponse addTest(Long topicId, TestRequest request) {
        if (request == null) {
            throw new BadRequestException("Test request cannot be null");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BadRequestException("Test title cannot be empty");
        }

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new TopicNotFoundException(topicId));

        Test test = new Test();
        test.setTitle(request.getTitle());
        test.setTopic(topic);

        // Sequence number atama
        Integer sequenceNumber = getNextSequenceNumber(topicId, request.getSequenceNumber());
        test.setSequenceNumber(sequenceNumber);

        test = testRepository.save(test);
        // Book data sayılarını asenkron olarak hesapla (ana işlemi bloklamaz)
        // ApplicationContext üzerinden çağır (self-invocation sorununu çözmek için)
        // bookId'yi transaction içinde al (lazy initialization sorununu önlemek için)
        Long bookId = test.getTopic().getBook().getId();
        BookAdminService bookAdminServiceProxy = applicationContext.getBean(BookAdminService.class);
        bookAdminServiceProxy.countBookDataAsync(bookId);
        return toTestResponse(test);
    }

    // READ
    @Transactional(readOnly = true)
    public List<TestResponse> getTestsByTopic(Long topicId) {
        return testRepository.findByTopicIdOrderBySequenceNumberAsc(topicId)
                .stream()
                .map(this::toTestResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TestResponse getTestById(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new TestNotFoundException(testId));
        return toTestResponse(test);
    }

    // UPDATE
    @Transactional
    public TestResponse updateTest(Long testId, TestRequest request) {
        if (request == null) {
            throw new BadRequestException("Test request cannot be null");
        }
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new TestNotFoundException(testId));

        if (request.getTitle() != null) {
            if (request.getTitle().isBlank()) {
                throw new BadRequestException("Test title cannot be empty");
            }
            test.setTitle(request.getTitle());
        }

        test = testRepository.save(test);
        // Book data sayılarını asenkron olarak hesapla (ana işlemi bloklamaz)
        // ApplicationContext üzerinden çağır (self-invocation sorununu çözmek için)
        // bookId'yi transaction içinde al (lazy initialization sorununu önlemek için)
        Long bookId = test.getTopic().getBook().getId();
        BookAdminService bookAdminServiceProxy = applicationContext.getBean(BookAdminService.class);
        bookAdminServiceProxy.countBookDataAsync(bookId);
        return toTestResponse(test);
    }

    // DELETE
    @Transactional
    public void deleteTest(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new TestNotFoundException(testId));
        Long bookId = test.getTopic().getBook().getId();
        testRepository.delete(test);
        // Book data sayılarını asenkron olarak hesapla (ana işlemi bloklamaz)
        // ApplicationContext üzerinden çağır (self-invocation sorununu çözmek için)
        BookAdminService bookAdminServiceProxy = applicationContext.getBean(BookAdminService.class);
        bookAdminServiceProxy.countBookDataAsync(bookId);
    }

    // --- HELPER METHOD ---
    private TestResponse toTestResponse(Test test) {
        TestResponse response = new TestResponse();
        response.setId(test.getId());
        response.setTitle(test.getTitle());
        response.setNumberOfQuestions(test.getNumberOfQuestions());
        response.setSequenceNumber(test.getSequenceNumber());
        return response;
    }

    // Helper: Otomatik sequenceNumber atama
    private Integer getNextSequenceNumber(Long topicId, Integer requestedSequenceNumber) {
        if (requestedSequenceNumber != null) {
            // Eğer belirli bir sequence number isteniyorsa, o numaranın kullanılıp
            // kullanılmadığını kontrol et
            if (requestedSequenceNumber < 1) {
                throw new BadRequestException("Sequence number must be greater than 0");
            }
            if (testRepository.existsByTopicIdAndSequenceNumber(topicId, requestedSequenceNumber)) {
                throw new ConflictException("Sequence number " + requestedSequenceNumber
                        + " is already in use. Please delete the existing test with this sequence number first.");
            }
            return requestedSequenceNumber;
        } else {
            // Otomatik olarak en küçük boş numarayı bul
            Integer maxSequence = testRepository.findMaxSequenceNumberByTopicId(topicId);
            if (maxSequence == null) {
                return 1; // İlk test
            }

            // 1'den başlayarak boş numarayı bul
            for (int i = 1; i <= maxSequence + 1; i++) {
                if (!testRepository.existsByTopicIdAndSequenceNumber(topicId, i)) {
                    return i;
                }
            }
            return maxSequence + 1; // Fallback
        }
    }
}
