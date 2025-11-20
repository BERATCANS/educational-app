package com.beratcan.app.service;

import com.beratcan.app.dto.response.TestResultResponse;
import com.beratcan.app.entity.*;
import com.beratcan.app.entity.enums.OptionLetter;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.QuestionNotFoundException;
import com.beratcan.app.exception.TestNotFoundException;
import com.beratcan.app.exception.TestResultNotFoundException;
import com.beratcan.app.exception.UnauthorizedOperationException;
import com.beratcan.app.exception.UserNotFoundException;
import com.beratcan.app.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TestResultService {

    private final TestResultRepository testResultRepository;
    private final QuestionResultRepository questionResultRepository;
    private final UserRepository userRepository;
    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;

    public TestResultService(TestResultRepository testResultRepository,
            QuestionResultRepository questionResultRepository,
            UserRepository userRepository,
            TestRepository testRepository,
            QuestionRepository questionRepository) {
        this.testResultRepository = testResultRepository;
        this.questionResultRepository = questionResultRepository;
        this.userRepository = userRepository;
        this.testRepository = testRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional
    public TestResult saveTestResult(Long userId, Long testId,
            Map<Long, OptionLetter> userAnswers,
            Integer totalTimeSeconds) {
        if (userAnswers == null || userAnswers.isEmpty()) {
            throw new BadRequestException("User answers cannot be null or empty");
        }
        if (totalTimeSeconds == null || totalTimeSeconds < 0) {
            throw new BadRequestException("Total time seconds must be a non-negative number");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new TestNotFoundException(testId));

        // Önce mevcut test sonucunu kontrol et
        TestResult existingTestResult = testResultRepository
                .findFirstByUserIdAndTestIdOrderByCompletedAtDesc(userId, testId)
                .orElse(null);

        TestResult testResult;
        if (existingTestResult != null) {
            // Mevcut test sonucunu güncelle
            testResult = existingTestResult;
            testResult.setTimeSpentSeconds(totalTimeSeconds);
            testResult.setCompletedAt(LocalDateTime.now());
        } else {
            // Yeni test sonucu oluştur
            testResult = new TestResult();
            testResult.setUser(user);
            testResult.setTest(test);
            testResult.setTimeSpentSeconds(totalTimeSeconds);
            testResult.setCompletedAt(LocalDateTime.now());
        }

        int correctCount = 0;
        // Toplam soru sayısını test'ten al - numberOfQuestions alanını kullan
        int totalQuestions = test.getNumberOfQuestions();

        // Her soru için cevabı kaydet
        for (Map.Entry<Long, OptionLetter> entry : userAnswers.entrySet()) {
            Long questionId = entry.getKey();
            OptionLetter selectedAnswer = entry.getValue();

            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new QuestionNotFoundException(questionId));

            boolean isCorrect = question.getCorrectAnswer().equals(selectedAnswer);
            if (isCorrect) {
                correctCount++;
            }

            // Mevcut QuestionResult'ı kontrol et (userId ve questionId ile)
            List<QuestionResult> existingQuestionResults = questionResultRepository
                    .findByUserIdAndQuestionId(userId, questionId);

            QuestionResult questionResult;
            if (!existingQuestionResults.isEmpty()) {
                // Mevcut QuestionResult'ı güncelle
                questionResult = existingQuestionResults.get(0); // En son olanı al
                questionResult.setTestResult(testResult);
                questionResult.setSelectedAnswer(selectedAnswer);
                questionResult.setIsCorrect(isCorrect);
                questionResult.setAnsweredAt(LocalDateTime.now());
            } else {
                // Yeni QuestionResult oluştur
                questionResult = new QuestionResult();
                questionResult.setTestResult(testResult);
                questionResult.setQuestion(question);
                questionResult.setSelectedAnswer(selectedAnswer);
                questionResult.setIsCorrect(isCorrect);
                questionResult.setAnsweredAt(LocalDateTime.now());
            }

            testResult.getQuestionResults().add(questionResult);
        }

        // Skorları hesapla
        testResult.setScore(correctCount);
        testResult.setTotalQuestions(totalQuestions);
        testResult.calculateSuccessRate();

        return testResultRepository.save(testResult);
    }

    public List<TestResult> getUserTestResults(Long userId) {
        return testResultRepository.findByUserIdOrderByCompletedAtDesc(userId);
    }

    public TestResult getTestResultById(Long testResultId) {
        return testResultRepository.findById(testResultId)
                .orElseThrow(() -> new TestResultNotFoundException(testResultId));
    }

    public TestResult getTestResultById(Long testResultId, Long userId) {
        TestResult testResult = testResultRepository.findById(testResultId)
                .orElseThrow(() -> new TestResultNotFoundException(testResultId));

        if (!testResult.getUser().getId().equals(userId)) {
            throw new UnauthorizedOperationException("You can only access your own test results");
        }

        return testResult;
    }

    // Kullanıcı istatistikleri
    public Double getUserAverageSuccessRate(Long userId) {
        return testResultRepository.findAverageSuccessRateByUserId(userId);
    }

    public Long getUserTotalTestCount(Long userId) {
        return testResultRepository.countTestsByUserId(userId);
    }

    // Kullanıcının belirli bir test için sonucunu getir
    public TestResult getUserTestResultByTestId(Long userId, Long testId) {
        return testResultRepository.findFirstByUserIdAndTestIdOrderByCompletedAtDesc(userId, testId)
                .orElse(null); // Eğer bulunamazsa null döner
    }

    // Kullanıcının belirli bir kitaptaki tüm test sonuçlarını getir
    public List<TestResultResponse> getUserTestResultsByBookId(Long userId, Long bookId) {
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }
        if (bookId == null) {
            throw new BadRequestException("Book ID cannot be null");
        }

        // Kullanıcının var olup olmadığını kontrol et
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Önce kullanıcının tüm test sonuçlarını al
        List<TestResult> allUserResults = testResultRepository.findByUserIdOrderByCompletedAtDesc(userId);

        List<TestResultResponse> responses = new ArrayList<>();

        // Her test sonucu için, o testin kitabı kontrol et
        for (TestResult testResult : allUserResults) {
            Test test = testResult.getTest();
            if (test != null && test.getTopic() != null && test.getTopic().getBook() != null) {
                if (test.getTopic().getBook().getId().equals(bookId)) {
                    // Bu test sonucu istenen kitaba ait
                    TestResultResponse response = new TestResultResponse();
                    response.setUserId(userId);
                    response.setTestId(test.getId());
                    response.setSuccessRate(testResult.getSuccessRate());
                    responses.add(response);
                }
            }
        }

        return responses;
    }
}
