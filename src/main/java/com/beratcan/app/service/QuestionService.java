package com.beratcan.app.service;

import com.beratcan.app.dto.request.QuestionRequest;
import com.beratcan.app.dto.response.QuestionResponse;
import com.beratcan.app.dto.response.TestResponse;
import com.beratcan.app.entity.Explanation;
import com.beratcan.app.entity.Hint;
import com.beratcan.app.entity.Question;
import com.beratcan.app.entity.Test;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.ConflictException;
import com.beratcan.app.exception.QuestionNotFoundException;
import com.beratcan.app.exception.TestNotFoundException;
import com.beratcan.app.repository.QuestionRepository;
import com.beratcan.app.repository.TestRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final TestRepository testRepository;
    private final BookAdminService bookAdminService;

    // CREATE
    public QuestionResponse addQuestion(Long testId, QuestionRequest request) {
        if (request == null) {
            throw new BadRequestException("Question request cannot be null");
        }
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new TestNotFoundException(testId));

        Question question = new Question();
        question.setTest(test);
        question.setImageUrl(request.getImageUrl());
        question.setAnswerNumber(request.getAnswerNumber() > 0 ? request.getAnswerNumber() : 5);
        question.setCorrectAnswer(request.getCorrectAnswer());
        question.setVideoUrl(request.getVideoUrl());

        // Sequence number atama
        Integer sequenceNumber = getNextSequenceNumber(testId, request.getSequenceNumber());
        question.setSequenceNumber(sequenceNumber);

        question = questionRepository.save(question);
        bookAdminService.countBookData(test.getTopic().getBook().getId());
        return toQuestionResponse(question);
    }

    // READ
    public List<QuestionResponse> getQuestionsByTest(Long testId) {
        return questionRepository.findByTestIdOrderBySequenceNumberAsc(testId)
                .stream()
                .map(this::toQuestionResponse)
                .collect(Collectors.toList());
    }

    public QuestionResponse getQuestionById(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
        return toQuestionResponse(question);
    }

    // UPDATE
    public QuestionResponse updateQuestion(Long questionId, QuestionRequest request) {
        if (request == null) {
            throw new BadRequestException("Question request cannot be null");
        }
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        if (request.getImageUrl() != null) {
            question.setImageUrl(request.getImageUrl());
        }
        if (request.getCorrectAnswer() != null) {
            question.setCorrectAnswer(request.getCorrectAnswer());
        }
        if (request.getAnswerNumber() > 0) {
            question.setAnswerNumber(request.getAnswerNumber());
        }
        if (request.getVideoUrl() != null) {
            question.setVideoUrl(request.getVideoUrl());
        }

        question = questionRepository.save(question);
        return toQuestionResponse(question);
    }

    // DELETE
    public void deleteQuestion(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
        questionRepository.delete(question);
        bookAdminService.countBookData(question.getTest().getTopic().getBook().getId());
    }

    @Transactional
    public void voteHint(Long questionId, int hintIndex, boolean like) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        List<Hint> hints = question.getHints();
        if (hints == null || hints.isEmpty()) {
            throw new BadRequestException("This question has no hints");
        }
        if (hintIndex < 0 || hintIndex >= hints.size()) {
            throw new BadRequestException(
                    "Invalid hint index: " + hintIndex + ". Valid range: 0 to " + (hints.size() - 1));
        }

        Hint hint = hints.get(hintIndex);
        if (like) {
            hint.setLikes(hint.getLikes() + 1);
        } else {
            hint.setDislikes(hint.getDislikes() + 1);
        }

        // Question entity'si cascade ile update edileceği için explicit save opsiyonel
        questionRepository.save(question);
    }

    @Transactional
    public void voteExplanation(Long questionId, boolean like) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        Explanation explanation = question.getExplanation();
        if (explanation == null) {
            throw new BadRequestException("This question has no explanation");
        }

        if (like) {
            explanation.setLikes(explanation.getLikes() + 1);
        } else {
            explanation.setDislikes(explanation.getDislikes() + 1);
        }

        questionRepository.save(question);
    }

    // Helper: Otomatik sequenceNumber atama
    private Integer getNextSequenceNumber(Long testId, Integer requestedSequenceNumber) {
        if (requestedSequenceNumber != null) {
            // Eğer belirli bir sequence number isteniyorsa, o numaranın kullanılıp
            // kullanılmadığını kontrol et
            if (requestedSequenceNumber < 1) {
                throw new BadRequestException("Sequence number must be greater than 0");
            }
            if (questionRepository.existsByTestIdAndSequenceNumber(testId, requestedSequenceNumber)) {
                throw new ConflictException("Sequence number " + requestedSequenceNumber
                        + " is already in use. Please delete the existing question with this sequence number first.");
            }
            return requestedSequenceNumber;
        } else {
            // Otomatik olarak en küçük boş numarayı bul
            Integer maxSequence = questionRepository.findMaxSequenceNumberByTestId(testId);
            if (maxSequence == null) {
                return 1; // İlk soru
            }

            // 1'den başlayarak boş numarayı bul
            for (int i = 1; i <= maxSequence + 1; i++) {
                if (!questionRepository.existsByTestIdAndSequenceNumber(testId, i)) {
                    return i;
                }
            }
            return maxSequence + 1; // Fallback
        }
    }

    // HELPER
    private QuestionResponse toQuestionResponse(Question question) {
        QuestionResponse response = new QuestionResponse();
        response.setId(question.getId());
        response.setImageUrl(question.getImageUrl());
        response.setCorrectAnswer(question.getCorrectAnswer());
        response.setAnswerNumber(question.getAnswerNumber());
        response.setVideoUrl(question.getVideoUrl());
        response.setSequenceNumber(question.getSequenceNumber());

        // Test bilgisini ekle
        if (question.getTest() != null) {
            TestResponse testResponse = new TestResponse();
            testResponse.setId(question.getTest().getId());
            testResponse.setTitle(question.getTest().getTitle());
            testResponse.setSequenceNumber(question.getTest().getSequenceNumber());
            response.setTest(testResponse);
        }

        // Handle null explanation safely
        if (question.getExplanation() != null) {
            response.setExplanation(question.getExplanation().getText());
        } else {
            response.setExplanation("");
        }

        // Handle hints safely
        if (question.getHints() != null && !question.getHints().isEmpty()) {
            response.setHints(
                    question.getHints().stream()
                            .map(Hint::getText)
                            .toList());
        } else {
            response.setHints(List.of());
        }

        return response;
    }

    public List<QuestionResponse> getFlaggedQuestions() {
        // Şimdilik boş liste döndür, daha sonra implement edilecek
        return List.of();
    }

    /**
     * AI tarafından üretilen ipucu ekle
     */
    @Transactional
    public void addHint(Long questionId, String hintText) {
        if (hintText == null || hintText.trim().isEmpty()) {
            throw new BadRequestException("Hint text cannot be null or empty");
        }
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        Hint hint = new Hint();
        hint.setText(hintText);
        hint.setLikes(0);
        hint.setDislikes(0);

        question.getHints().add(hint);
        questionRepository.save(question);
    }

    /**
     * AI tarafından üretilen çözüm açıklaması ekle
     */
    @Transactional
    public void addExplanation(Long questionId, String explanationText) {
        if (explanationText == null || explanationText.trim().isEmpty()) {
            throw new BadRequestException("Explanation text cannot be null or empty");
        }
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        Explanation explanation = new Explanation();
        explanation.setText(explanationText);
        explanation.setLikes(0);
        explanation.setDislikes(0);

        question.setExplanation(explanation);
        questionRepository.save(question);
    }
}
