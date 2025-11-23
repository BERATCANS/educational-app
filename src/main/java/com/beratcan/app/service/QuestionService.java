package com.beratcan.app.service;

import com.beratcan.app.dto.request.QuestionRequest;
import com.beratcan.app.dto.response.QuestionAnalysisResult;
import com.beratcan.app.dto.response.QuestionResponse;
import com.beratcan.app.dto.response.TestResponse;
import com.beratcan.app.entity.Explanation;
import com.beratcan.app.entity.Hint;
import com.beratcan.app.entity.Question;
import com.beratcan.app.entity.Test;
import com.beratcan.app.entity.enums.OptionLetter;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.ConflictException;
import com.beratcan.app.exception.QuestionNotFoundException;
import com.beratcan.app.exception.TestNotFoundException;
import com.beratcan.app.repository.QuestionRepository;
import com.beratcan.app.repository.TestRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
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
     * Soru oluştur (görsel ile) + Otomatik AI analiz yap
     * 
     * @param file           Görsel dosyası
     * @param testId         Test ID
     * @param correctAnswer  Doğru cevap
     * @param answerNumber   Cevap sayısı
     * @param videoUrl       Video URL (opsiyonel)
     * @param sequenceNumber Sequence number (opsiyonel)
     * @return QuestionResponse (hints ve explanation dahil)
     */
    @Transactional
    public QuestionResponse addQuestionWithImageAndAI(
            MultipartFile file,
            Long testId,
            String correctAnswer,
            int answerNumber,
            String videoUrl,
            Integer sequenceNumber) {

        log.info("Soru oluşturma başlatıldı: testId={}, correctAnswer={}", testId, correctAnswer);

        // 1. Görseli kaydet
        String filename = fileService.saveImageFile(file);
        log.info("Görsel kaydedildi: {}", filename);

        // 2. QuestionRequest oluştur
        QuestionRequest request = new QuestionRequest();
        request.setImageUrl(filename);
        request.setCorrectAnswer(OptionLetter.valueOf(correctAnswer.toUpperCase()));
        request.setAnswerNumber(answerNumber);
        request.setVideoUrl(videoUrl);
        request.setSequenceNumber(sequenceNumber);

        // 3. Soruyu oluştur
        QuestionResponse question = addQuestion(testId, request);
        log.info("Soru oluşturuldu: questionId={}", question.getId());

        // 4. Otomatik AI analiz yap ve hints/explanation ekle
        analyzeQuestionWithAI(question.getId());

        // 5. Güncel soruyu getir (hints ve explanation dahil)
        return getQuestionById(question.getId());
    }

    /**
     * Soru için AI analiz yap ve hints/explanation ekle
     * 
     * @param questionId Soru ID
     */
    @Transactional
    private void analyzeQuestionWithAI(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        log.info("AI analiz başlatıldı: questionId={}", questionId);

        try {
            // 1. Görseli oku
            File imageFile = new File(uploadDir + question.getImageUrl());
            if (!imageFile.exists()) {
                throw new BadRequestException("Görsel dosyası bulunamadı: " + question.getImageUrl());
            }

            // 2. AI analiz yap
            QuestionAnalysisResult analysis = aiAnalyzer.analyzeQuestion(imageFile);
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

        } catch (Exception e) {
            log.error("AI analiz hatası: questionId={}", questionId, e);
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
