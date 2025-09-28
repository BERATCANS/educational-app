package com.beratcan.app.service;

import com.beratcan.app.entity.*;
import com.beratcan.app.entity.enums.OptionLetter;
import com.beratcan.app.entity.enums.TestStatus;
import com.beratcan.app.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));

        // Test sonucunu oluştur
        TestResult testResult = new TestResult();
        testResult.setUser(user);
        testResult.setTest(test);
        testResult.setTimeSpentSeconds(totalTimeSeconds);
        testResult.setStatus(TestStatus.COMPLETED);
        testResult.setCompletedAt(LocalDateTime.now());

        int correctCount = 0;
        int totalQuestions = userAnswers.size();

        // Her soru için cevabı kaydet
        for (Map.Entry<Long, OptionLetter> entry : userAnswers.entrySet()) {
            Long questionId = entry.getKey();
            OptionLetter selectedAnswer = entry.getValue();

            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new RuntimeException("Question not found"));

            boolean isCorrect = question.getCorrectAnswer().equals(selectedAnswer);
            if (isCorrect) {
                correctCount++;
            }

            QuestionResult questionResult = new QuestionResult();
            questionResult.setTestResult(testResult);
            questionResult.setQuestion(question);
            questionResult.setSelectedAnswer(selectedAnswer);
            questionResult.setIsCorrect(isCorrect);
            questionResult.setAnsweredAt(LocalDateTime.now());

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
                .orElseThrow(() -> new RuntimeException("Test result not found"));
    }

    // Kullanıcı istatistikleri
    public Double getUserAverageSuccessRate(Long userId) {
        return testResultRepository.findAverageSuccessRateByUserId(userId);
    }

    public Long getUserTotalTestCount(Long userId) {
        return testResultRepository.countTestsByUserId(userId);
    }
}
