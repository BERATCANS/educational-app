package com.beratcan.app.service;

import com.beratcan.app.dto.request.CreateQuestionRequest;
import com.beratcan.app.dto.request.QuestionRequest;
import com.beratcan.app.dto.request.UpdateQuestionRequest;
import com.beratcan.app.dto.response.AIAnalysisStatusResponse;
import com.beratcan.app.dto.response.QuestionAnalysisResult;
import com.beratcan.app.dto.response.QuestionResponse;
import com.beratcan.app.dto.response.TestResponse;
import com.beratcan.app.entity.Explanation;
import com.beratcan.app.entity.Hint;
import com.beratcan.app.entity.Question;
import com.beratcan.app.entity.Test;
import com.beratcan.app.entity.enums.AIAnalysisStatus;
import com.beratcan.app.entity.enums.ExamType;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.ConflictException;
import com.beratcan.app.exception.QuestionNotFoundException;
import com.beratcan.app.exception.TestNotFoundException;
import com.beratcan.app.repository.QuestionRepository;
import com.beratcan.app.repository.TestRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final TestRepository testRepository;
    private final BookAdminService bookAdminService;
    private final AIQuestionAnalyzer aiAnalyzer;
    private final FileService fileService;

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${file.upload.dir:uploads/}")
    private String uploadDir;

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

        // ExamType'ı denormalize olarak set et (performans için)
        ExamType examType = extractExamTypeFromTest(test);
        question.setExamType(examType);

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

    /**
     * Soru için AI analiz durumunu getir
     * 
     * @param questionId Soru ID
     * @return AIAnalysisStatusResponse
     */
    public AIAnalysisStatusResponse getAIAnalysisStatus(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        AIAnalysisStatus status = question.getAiAnalysisStatus() != null
                ? question.getAiAnalysisStatus()
                : AIAnalysisStatus.PENDING;

        String message = switch (status) {
            case PENDING -> "AI analiz henüz başlamadı";
            case PROCESSING -> "AI analiz devam ediyor, lütfen bekleyin";
            case COMPLETED -> "AI analiz tamamlandı";
            case FAILED -> "AI analiz başarısız oldu";
        };

        return new AIAnalysisStatusResponse(questionId, status, message);
    }

    // UPDATE
    @Transactional
    public QuestionResponse updateQuestion(Long questionId, UpdateQuestionRequest request) {
        if (request == null) {
            throw new BadRequestException("UpdateQuestionRequest cannot be null");
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        // ✅ Yeni görsel yüklenecekse
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            // Eski dosyayı sil
            if (question.getImageUrl() != null) {
                try {
                    fileService.deleteFile(question.getImageUrl());
                    log.info("Eski görsel silindi: {}", question.getImageUrl());
                } catch (Exception e) {
                    log.warn("Eski görsel silinirken hata oluştu: {}", question.getImageUrl(), e);
                    // Devam et, yeni dosyayı yükle
                }
            }
            // Yeni dosyayı yükle
            String newImageUrl = fileService.saveFile(request.getImage());
            question.setImageUrl(newImageUrl);
            log.info("Yeni görsel yüklendi: {}", newImageUrl);
        }

        // ✅ Diğer alanları güncelle (partial update - sadece null değilse)
        if (request.getCorrectAnswer() != null) {
            question.setCorrectAnswer(request.getCorrectAnswer());
        }
        if (request.getAnswerNumber() != null && request.getAnswerNumber() > 0) {
            question.setAnswerNumber(request.getAnswerNumber());
        }
        if (request.getVideoUrl() != null) {
            question.setVideoUrl(request.getVideoUrl());
        }
        if (request.getSequenceNumber() != null) {
            // Sequence number validasyonu - aynı test içinde unique olmalı
            Long testId = question.getTest().getId();
            if (questionRepository.existsByTestIdAndSequenceNumber(testId, request.getSequenceNumber())) {
                // Mevcut sorunun sequence number'ı değiştiriliyorsa ve başka bir soru bu
                // numarayı kullanıyorsa
                if (!request.getSequenceNumber().equals(question.getSequenceNumber())) {
                    throw new ConflictException("Sequence number " + request.getSequenceNumber()
                            + " is already in use for this test. Please choose a different sequence number.");
                }
            }
            question.setSequenceNumber(request.getSequenceNumber());
        }

        question = questionRepository.save(question);
        log.info("Soru güncellendi: questionId={}", questionId);
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

    /**
     * Test'ten ExamType çıkar (Test → Topic → Book → ExamType)
     */
    private ExamType extractExamTypeFromTest(Test test) {
        if (test == null || test.getTopic() == null || test.getTopic().getBook() == null) {
            return ExamType.DIGER; // Default
        }
        ExamType examType = test.getTopic().getBook().getExamType();
        return examType != null ? examType : ExamType.DIGER;
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
        // Database'de filename var, response'da URL oluştur
        // Eğer zaten URL formatındaysa (eski kayıtlar için) olduğu gibi bırak
        String imageUrl = question.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.startsWith("api/files/")) {
            response.setImageUrl("api/files/" + imageUrl);
        } else {
            response.setImageUrl(imageUrl);
        }
        response.setCorrectAnswer(question.getCorrectAnswer());
        response.setAnswerNumber(question.getAnswerNumber());
        response.setVideoUrl(question.getVideoUrl());
        response.setSequenceNumber(question.getSequenceNumber());
        response.setAiAnalysisStatus(question.getAiAnalysisStatus() != null
                ? question.getAiAnalysisStatus()
                : AIAnalysisStatus.PENDING);

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

        // Handle hints safely - sequenceNumber'a göre sırala
        if (question.getHints() != null && !question.getHints().isEmpty()) {
            response.setHints(
                    question.getHints().stream()
                            .sorted((h1, h2) -> Integer.compare(h1.getSequenceNumber(), h2.getSequenceNumber()))
                            .map(Hint::getText)
                            .toList());
        } else {
            response.setHints(List.of());
        }

        return response;
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
        // Sequence number'ı mevcut hint sayısına göre set et (0, 1, 2, ...)
        hint.setSequenceNumber(question.getHints().size());

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

    /**
     * Soru oluştur (görsel ile) + Asenkron AI analiz başlat
     * 
     * @param request CreateQuestionRequest DTO (görsel, testId, correctAnswer, vb.)
     * @return QuestionResponse (AI analiz henüz tamamlanmamış olabilir, status
     *         kontrol edilmeli)
     */
    @Transactional
    public QuestionResponse addQuestionWithImageAndAI(CreateQuestionRequest request) {
        if (request == null) {
            throw new BadRequestException("CreateQuestionRequest cannot be null");
        }

        log.info("Soru oluşturma başlatıldı: testId={}, correctAnswer={}",
                request.getTestId(), request.getCorrectAnswer());

        // 1. Görseli kaydet
        String imageUrl = fileService.saveFile(request.getImage());
        log.info("Görsel kaydedildi: {}", imageUrl);

        // 2. QuestionRequest oluştur
        QuestionRequest questionRequest = new QuestionRequest();
        questionRequest.setImageUrl(imageUrl);
        questionRequest.setCorrectAnswer(request.getCorrectAnswer());
        questionRequest.setAnswerNumber(request.getAnswerNumber());
        questionRequest.setVideoUrl(request.getVideoUrl());
        questionRequest.setSequenceNumber(request.getSequenceNumber());

        // 3. Soruyu oluştur
        QuestionResponse question = addQuestion(request.getTestId(), questionRequest);
        log.info("Soru oluşturuldu: questionId={}", question.getId());

        // 4. AI analiz durumunu PROCESSING olarak işaretle
        Question questionEntity = questionRepository.findById(question.getId())
                .orElseThrow(() -> new QuestionNotFoundException(question.getId()));
        questionEntity.setAiAnalysisStatus(AIAnalysisStatus.PROCESSING);
        questionRepository.save(questionEntity);

        // 5. AI analizini arka planda başlat (asenkron)
        // ApplicationContext üzerinden çağır (self-invocation sorununu çözmek için)
        QuestionService self = applicationContext.getBean(QuestionService.class);
        self.analyzeQuestionWithAIAsync(question.getId());

        // 6. Hemen response dön (hints ve explanation henüz yok, status PROCESSING)
        return getQuestionById(question.getId());
    }

    /**
     * Soru için AI analiz yap ve hints/explanation ekle (asenkron)
     * 
     * @param questionId Soru ID
     * @return CompletableFuture<Void>
     */
    @Async("aiAnalysisExecutor")
    public CompletableFuture<Void> analyzeQuestionWithAIAsync(Long questionId) {
        log.info("AI analiz başlatıldı (asenkron): questionId={}", questionId);

        try {
            // ApplicationContext üzerinden çağır (transaction'ın düzgün çalışması için)
            // @Async zaten proxy üzerinden çalışır, ama @Transactional metod için proxy
            // gerekli
            QuestionService self = applicationContext.getBean(QuestionService.class);
            self.analyzeQuestionWithAIInternal(questionId);
            log.info("AI analiz tamamlandı (asenkron): questionId={}", questionId);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("AI analiz hatası (asenkron): questionId={}, error={}", questionId, e.getMessage(), e);
            // Hata durumunda status'u FAILED olarak güncelle
            try {
                QuestionService self = applicationContext.getBean(QuestionService.class);
                self.updateAIAnalysisStatus(questionId, AIAnalysisStatus.FAILED);
            } catch (Exception ex) {
                log.error("Status güncelleme hatası (asenkron): questionId={}", questionId, ex);
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * AI analiz işlemini gerçekleştir (transaction içinde)
     * Public yapıldı - ApplicationContext üzerinden çağrılabilmesi için
     */
    @Transactional
    public void analyzeQuestionWithAIInternal(Long questionId) {
        log.info("AI analiz internal metod başlatıldı: questionId={}", questionId);

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        try {
            // 1. Görseli oku
            File imageFile = new File(uploadDir + question.getImageUrl());
            if (!imageFile.exists()) {
                log.error("Görsel dosyası bulunamadı: questionId={}, imageUrl={}, fullPath={}",
                        questionId, question.getImageUrl(), uploadDir + question.getImageUrl());
                updateAIAnalysisStatus(questionId, AIAnalysisStatus.FAILED);
                return;
            }

            log.info("Görsel dosyası bulundu: questionId={}, fileSize={} bytes",
                    questionId, imageFile.length());

            // 2. AI analiz yap (questionId ile ExamType algılanacak)
            QuestionAnalysisResult analysis = aiAnalyzer.analyzeQuestion(imageFile, questionId);
            log.info("AI analiz tamamlandı: questionId={}", questionId);

            // 3. Question'ı tekrar yükle (transaction içinde fresh state için)
            question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new QuestionNotFoundException(questionId));

            // 4. Mevcut hints'i temizle
            question.getHints().clear();

            // 5. Yeni hints ekle (transaction içinde)
            if (analysis.getHint1() != null && !analysis.getHint1().trim().isEmpty()) {
                Hint hint1 = new Hint();
                hint1.setText(analysis.getHint1().trim());
                hint1.setSequenceNumber(0);
                hint1.setLikes(0);
                hint1.setDislikes(0);
                question.getHints().add(hint1);
            }
            if (analysis.getHint2() != null && !analysis.getHint2().trim().isEmpty()) {
                Hint hint2 = new Hint();
                hint2.setText(analysis.getHint2().trim());
                hint2.setSequenceNumber(1);
                hint2.setLikes(0);
                hint2.setDislikes(0);
                question.getHints().add(hint2);
            }
            if (analysis.getHint3() != null && !analysis.getHint3().trim().isEmpty()) {
                Hint hint3 = new Hint();
                hint3.setText(analysis.getHint3().trim());
                hint3.setSequenceNumber(2);
                hint3.setLikes(0);
                hint3.setDislikes(0);
                question.getHints().add(hint3);
            }
            log.info("3 ipucu eklendi: questionId={}, hintCount={}", questionId, question.getHints().size());

            // 6. Explanation ekle/güncelle
            if (analysis.getSolution() != null && !analysis.getSolution().trim().isEmpty()) {
                if (question.getExplanation() == null) {
                    Explanation explanation = new Explanation();
                    explanation.setText(analysis.getSolution().trim());
                    explanation.setLikes(0);
                    explanation.setDislikes(0);
                    question.setExplanation(explanation);
                } else {
                    question.getExplanation().setText(analysis.getSolution().trim());
                }
            }
            log.info("Çözüm eklendi: questionId={}", questionId);

            // 7. Status'u COMPLETED olarak güncelle
            question.setAiAnalysisStatus(AIAnalysisStatus.COMPLETED);
            questionRepository.save(question);
            log.info("AI analiz başarıyla tamamlandı: questionId={}, status=COMPLETED", questionId);

        } catch (Exception e) {
            log.error("AI analiz hatası (internal): questionId={}, error={}", questionId, e.getMessage(), e);
            // Status'u FAILED olarak güncelle
            updateAIAnalysisStatus(questionId, AIAnalysisStatus.FAILED);
            // Exception'ı tekrar fırlatma, async metod zaten yakalayacak
        }
    }

    /**
     * AI analiz status'unu güncelle (transaction içinde)
     */
    @Transactional
    public void updateAIAnalysisStatus(Long questionId, AIAnalysisStatus status) {
        try {
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new QuestionNotFoundException(questionId));
            question.setAiAnalysisStatus(status);
            questionRepository.save(question);
            log.info("AI analiz status güncellendi: questionId={}, status={}", questionId, status);
        } catch (Exception e) {
            log.error("Status güncelleme hatası: questionId={}, status={}", questionId, status, e);
        }
    }

    /**
     * Soru için AI analiz yap ve hints/explanation ekle (senkron - eski metod,
     * geriye dönük uyumluluk için)
     * 
     * @param questionId Soru ID
     */
    @Transactional
    public void analyzeQuestionWithAI(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        log.info("AI analiz başlatıldı: questionId={}", questionId);

        try {
            // 1. Görseli oku
            File imageFile = new File(uploadDir + question.getImageUrl());
            if (!imageFile.exists()) {
                throw new BadRequestException("Görsel dosyası bulunamadı: " + question.getImageUrl());
            }

            // 2. AI analiz yap (questionId ile ExamType algılanacak)
            QuestionAnalysisResult analysis = aiAnalyzer.analyzeQuestion(imageFile, questionId);
            log.info("AI analiz tamamlandı: questionId={}", questionId);

            // 3. Mevcut hints'i temizle
            question.getHints().clear();

            // 4. Yeni hints ekle
            addHint(questionId, analysis.getHint1());
            addHint(questionId, analysis.getHint2());
            addHint(questionId, analysis.getHint3());
            log.info("3 ipucu eklendi: questionId={}", questionId);

            // 5. Explanation ekle/güncelle
            addExplanation(questionId, analysis.getSolution());
            log.info("Çözüm eklendi: questionId={}", questionId);

            // 6. Status'u COMPLETED olarak güncelle
            question.setAiAnalysisStatus(AIAnalysisStatus.COMPLETED);
            questionRepository.save(question);

        } catch (Exception e) {
            log.error("AI analiz hatası: questionId={}", questionId, e);
            question.setAiAnalysisStatus(AIAnalysisStatus.FAILED);
            questionRepository.save(question);
            throw new BadRequestException("AI analiz başarısız: " + e.getMessage());
        }
    }

    /**
     * Soru için hints ve explanation'ı birlikte güncelle
     * 
     * @param questionId  Soru ID
     * @param hints       Yeni hints listesi
     * @param explanation Yeni explanation
     */
    @Transactional
    public void updateAnalysis(Long questionId, List<String> hints, String explanation) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        log.info("Analysis güncelleme başlatıldı: questionId={}", questionId);

        // 1. Mevcut hints'i temizle
        question.getHints().clear();

        // 2. Yeni hints'i ekle (sequenceNumber ile)
        if (hints != null && !hints.isEmpty()) {
            for (int i = 0; i < hints.size(); i++) {
                String hintText = hints.get(i);
                if (hintText != null && !hintText.trim().isEmpty()) {
                    Hint hint = new Hint();
                    hint.setText(hintText.trim());
                    hint.setSequenceNumber(i);
                    hint.setLikes(0);
                    hint.setDislikes(0);
                    question.getHints().add(hint);
                }
            }
        }

        // 3. Explanation'ı güncelle/oluştur
        if (explanation != null && !explanation.trim().isEmpty()) {
            if (question.getExplanation() == null) {
                Explanation exp = new Explanation();
                exp.setText(explanation.trim());
                exp.setLikes(0);
                exp.setDislikes(0);
                question.setExplanation(exp);
            } else {
                question.getExplanation().setText(explanation.trim());
            }
        } else {
            // Explanation boşsa sil
            question.setExplanation(null);
        }

        questionRepository.save(question);
        log.info("Analysis güncellendi: questionId={}, hintsCount={}",
                questionId, hints != null ? hints.size() : 0);
    }
}
