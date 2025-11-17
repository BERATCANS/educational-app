package com.beratcan.app.service;

import com.beratcan.app.dto.request.QuestionRequest;
import com.beratcan.app.dto.request.TestRequest;
import com.beratcan.app.dto.response.TestResponse;
import com.beratcan.app.entity.Question;
import com.beratcan.app.entity.Test;
import com.beratcan.app.entity.Topic;
import com.beratcan.app.repository.QuestionRepository;
import com.beratcan.app.repository.TestRepository;
import com.beratcan.app.repository.TopicRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestService {

    private final TestRepository testRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final BookAdminService bookAdminService;

    // CREATE
    public TestResponse addTest(Long topicId, TestRequest request) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic not found"));

        Test test = new Test();
        test.setTitle(request.getTitle());
        test.setTopic(topic);

        // Sequence number atama
        Integer sequenceNumber = getNextSequenceNumber(topicId, request.getSequenceNumber());
        test.setSequenceNumber(sequenceNumber);

        test = testRepository.save(test);

        if (request.getQuestions() != null) {
            for (QuestionRequest qr : request.getQuestions()) {
                Question question = new Question();
                question.setTest(test);
                question.setImageUrl(qr.getImageUrl());
                question.setCorrectAnswer(qr.getCorrectAnswer());
                question.setAnswerNumber(qr.getAnswerNumber() == 0 ? 5 : qr.getAnswerNumber());
                question.setVideoUrl(qr.getVideoUrl());
                questionRepository.save(question);
                test.getQuestions().add(question);
            }
            test = testRepository.save(test);
        }
        bookAdminService.countBookData(test.getTopic().getBook().getId());
        return toTestResponse(test);
    }

    // READ
    public List<TestResponse> getTestsByTopic(Long topicId) {
        return testRepository.findByTopicIdOrderBySequenceNumberAsc(topicId)
                .stream()
                .map(this::toTestResponse)
                .collect(Collectors.toList());
    }

    public TestResponse getTestById(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));
        return toTestResponse(test);
    }

    // UPDATE
    public TestResponse updateTest(Long testId, TestRequest request) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));

        if (request.getTitle() != null) {
            test.setTitle(request.getTitle());
        }

        if (request.getQuestions() != null) {
            for (QuestionRequest qr : request.getQuestions()) {
                Question question = new Question();
                question.setTest(test);
                question.setImageUrl(qr.getImageUrl());
                question.setCorrectAnswer(qr.getCorrectAnswer());
                question.setAnswerNumber(qr.getAnswerNumber() == 0 ? 5 : qr.getAnswerNumber());
                question.setVideoUrl(qr.getVideoUrl());
                questionRepository.save(question);
                test.getQuestions().add(question);
            }
        }

        test = testRepository.save(test);
        bookAdminService.countBookData(test.getTopic().getBook().getId());
        return toTestResponse(test);
    }

    // DELETE
    public void deleteTest(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));
        testRepository.delete(test);
        bookAdminService.countBookData(test.getTopic().getBook().getId());
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
            if (testRepository.existsByTopicIdAndSequenceNumber(topicId, requestedSequenceNumber)) {
                throw new RuntimeException("Sequence number " + requestedSequenceNumber
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
