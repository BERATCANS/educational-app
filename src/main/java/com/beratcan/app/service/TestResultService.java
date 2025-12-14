package com.beratcan.app.service;

import com.beratcan.app.dto.response.PageResponse;
import com.beratcan.app.dto.response.QuestionResultResponse;
import com.beratcan.app.dto.response.TestResultDetailResponse;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
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
    public TestResultDetailResponse saveTestResult(Long userId, Long testId,
            Map<Long, OptionLetter> userAnswers,
            Integer totalTimeSeconds) {
        long startTime = System.currentTimeMillis();

        if (userAnswers == null || userAnswers.isEmpty()) {
            throw new BadRequestException("User answers cannot be null or empty");
        }
        if (totalTimeSeconds == null || totalTimeSeconds < 0) {
            throw new BadRequestException("Total time seconds must be a non-negative number");
        }

        log.info("EVENT: Test submission started | userId={} | testId={} | questionCount={}",
                userId, testId, userAnswers.size());

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

        Double successRate = testResult.getSuccessRate();
        log.info("EVENT: Test scored | userId={} | testId={} | score={}/{} | successRate={}%",
                userId, testId, correctCount, totalQuestions,
                successRate != null ? String.format("%.2f", successRate) : "N/A");

        TestResult savedTestResult = testResultRepository.save(testResult);

        // Kullanıcının günlük çözülen soru sayısını güncelle
        updateDailySolvedQuestions(user, totalQuestions);

        // QuestionResults'ı da yüklemek için yeniden çek (lazy loading sorununu önlemek
        // için)
        TestResult testResultWithQuestionResults = testResultRepository
                .findByIdWithTestUserAndQuestionResults(savedTestResult.getId())
                .orElse(savedTestResult);

        long duration = System.currentTimeMillis() - startTime;
        log.info("EVENT: Test submission completed | testResultId={} | userId={} | testId={} | duration={}ms",
                savedTestResult.getId(), userId, testId, duration);

        // Performance warning for slow operations (5 second threshold)
        if (duration > 5000) {
            log.warn("PERF: Test submission slow | duration={}ms | threshold=5000ms | userId={} | testId={}",
                    duration, userId, testId);
        }

        // Transaction içinde DTO'ya çevir (lazy loading sorununu önlemek için)
        return mapToDetailResponse(testResultWithQuestionResults);
    }

    /**
     * Kullanıcının günlük çözülen soru sayısını günceller
     * Sadece bugünkü sayıya ekler, tarih kontrolü yapmaz
     * (Tarih kontrolü getCurrentUser/getQuestionTarget'da yapılır)
     * 
     * @param user            Kullanıcı entity
     * @param solvedQuestions Çözülen soru sayısı
     */
    private void updateDailySolvedQuestions(User user, int solvedQuestions) {
        // Sadece bugünkü sayıya ekle (tarih kontrolü yapmadan)
        int currentSolved = user.getDailySolvedQuestions() != null ? user.getDailySolvedQuestions() : 0;
        user.setDailySolvedQuestions(currentSolved + solvedQuestions);
        userRepository.save(user);
    }

    /**
     * Kullanıcının tüm test sonuçlarını pagination ile getirir
     * DB seviyesinde pagination yapar, memory'ye yük bindirmez
     * JOIN FETCH ile N+1 query problemini önler
     * Liste görünümü için QuestionResults yüklenmez (performans optimizasyonu)
     */
    @Transactional(readOnly = true)
    public PageResponse<TestResultDetailResponse> getUserTestResults(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        // QuestionResults yüklemeden hızlı query kullan (liste görünümü için yeterli)
        Page<TestResult> testResultPage = testResultRepository
                .findByUserIdOrderByCompletedAtDesc(userId, pageable);

        // Entity'leri DTO'ya çevir (transaction içinde, lazy loading sorununu önlemek
        // için)
        // QuestionResults null olacak (liste görünümü için gerekli değil)
        List<TestResultDetailResponse> testResultResponses = testResultPage.getContent().stream()
                .map(this::mapToDetailResponse)
                .toList();

        return PageResponse.of(
                testResultResponses,
                page,
                size,
                testResultPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public TestResultDetailResponse getTestResultById(Long testResultId) {
        // JOIN FETCH ile test, user ve questionResults ilişkilerini tek query'de yükle
        TestResult testResult = testResultRepository.findByIdWithTestUserAndQuestionResults(testResultId)
                .orElseThrow(() -> new TestResultNotFoundException(testResultId));

        // Transaction içinde DTO'ya çevir (lazy loading sorununu önlemek için)
        return mapToDetailResponse(testResult);
    }

    @Transactional(readOnly = true)
    public TestResultDetailResponse getTestResultById(Long testResultId, Long userId) {
        // JOIN FETCH ile test, user ve questionResults ilişkilerini tek query'de yükle
        TestResult testResult = testResultRepository.findByIdWithTestUserAndQuestionResults(testResultId)
                .orElseThrow(() -> new TestResultNotFoundException(testResultId));

        // Authorization kontrolü (User zaten JOIN FETCH ile yüklü)
        if (!testResult.getUser().getId().equals(userId)) {
            throw new UnauthorizedOperationException("You can only access your own test results");
        }

        // Transaction içinde DTO'ya çevir (lazy loading sorununu önlemek için)
        return mapToDetailResponse(testResult);
    }

    // Kullanıcı istatistikleri
    @Transactional(readOnly = true)
    public Double getUserAverageSuccessRate(Long userId) {
        return testResultRepository.findAverageSuccessRateByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Long getUserTotalTestCount(Long userId) {
        return testResultRepository.countTestsByUserId(userId);
    }

    // Kullanıcının belirli bir test için sonucunu getir
    @Transactional(readOnly = true)
    public TestResultDetailResponse getUserTestResultByTestId(Long userId, Long testId) {
        // JOIN FETCH ile test ve user ilişkilerini tek query'de yükle
        java.util.List<TestResult> results = testResultRepository
                .findFirstByUserIdAndTestIdWithTestAndUser(userId, testId);

        // En son sonucu döndür (ORDER BY completedAt DESC ile sıralı)
        if (results.isEmpty()) {
            return null;
        }

        TestResult testResult = results.get(0);
        return mapToDetailResponse(testResult);
    }

    /**
     * Kullanıcının belirli bir kitaptaki test sonuçlarını getirir
     * JOIN FETCH ile N+1 query problemini önler
     */
    @Transactional(readOnly = true)
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

        // DB seviyesinde filtreleme (pagination yok)
        List<TestResult> testResults = testResultRepository
                .findAllByUserIdAndBookId(userId, bookId);

        // DTO'ya çevir (sadece testId ve successRate - basit mapping)
        return testResults.stream()
                .map(testResult -> {
                    TestResultResponse response = new TestResultResponse();
                    response.setTestId(testResult.getTest().getId());
                    response.setSuccessRate(testResult.getSuccessRate());
                    return response;
                })
                .toList();
    }

    /**
     * TestResult entity'sini TestResultDetailResponse DTO'suna çevirir (detaylı
     * bilgiler)
     * Transaction içinde çağrılmalı (lazy loading sorununu önlemek için)
     */
    private TestResultDetailResponse mapToDetailResponse(TestResult testResult) {
        TestResultDetailResponse response = new TestResultDetailResponse();
        response.setId(testResult.getId());
        response.setScore(testResult.getScore());
        response.setTotalQuestions(testResult.getTotalQuestions());
        response.setSuccessRate(testResult.getSuccessRate());
        response.setTimeSpentSeconds(testResult.getTimeSpentSeconds());
        response.setCompletedAt(testResult.getCompletedAt());

        // Test bilgilerini direkt field'lara ekle (iç içe DTO yerine)
        if (testResult.getTest() != null) {
            response.setTestId(testResult.getTest().getId());
            response.setTestTitle(testResult.getTest().getTitle());
            response.setTestNumberOfQuestions(testResult.getTest().getNumberOfQuestions());
            response.setTestSequenceNumber(testResult.getTest().getSequenceNumber());
        }

        // QuestionResults'ı DTO'ya çevir
        if (testResult.getQuestionResults() != null && !testResult.getQuestionResults().isEmpty()) {
            List<QuestionResultResponse> questionResultResponses = testResult.getQuestionResults().stream()
                    .map(qr -> {
                        QuestionResultResponse qrResponse = new QuestionResultResponse();
                        qrResponse.setId(qr.getId());
                        qrResponse.setQuestionId(qr.getQuestion() != null ? qr.getQuestion().getId() : null);
                        qrResponse.setSelectedAnswer(qr.getSelectedAnswer());
                        qrResponse.setIsCorrect(qr.getIsCorrect());
                        qrResponse.setTimeSpentSeconds(qr.getTimeSpentSeconds());
                        qrResponse.setAnsweredAt(qr.getAnsweredAt());
                        return qrResponse;
                    })
                    .toList();
            response.setQuestionResults(questionResultResponses);
        }

        return response;
    }
}
