package com.beratcan.app.controller;

import com.beratcan.app.dto.request.QuestionRequest;
import com.beratcan.app.dto.response.QuestionAnalysisResult;
import com.beratcan.app.dto.response.QuestionResponse;
import com.beratcan.app.entity.enums.OptionLetter;
import com.beratcan.app.service.AIQuestionAnalyzer;
import com.beratcan.app.service.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final AIQuestionAnalyzer aiAnalyzer;
    private final QuestionService questionService;
    
    private final String uploadDir = "uploads/"; // proje içi klasör

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // Dosya adı oluştur
            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir + filename);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, file.getBytes());

            // Dosya URL'si
            String fileUrl = "/api/files/" + filename;
            return ResponseEntity.ok(fileUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Dosya yüklenemedi.");
        }
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) throws IOException {
        Path filePath = Paths.get(uploadDir + filename);
        Resource resource = new UrlResource(filePath.toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    /**
     * AI ile soru yükleme - Otomatik ipucu ve çözüm üretimi
     */
    @PostMapping("/upload-question-with-ai")
    public ResponseEntity<?> uploadQuestionWithAI(
            @RequestParam("file") MultipartFile file,
            @RequestParam("testId") Long testId,
            @RequestParam("correctAnswer") String correctAnswer,
            @RequestParam(value = "answerNumber", defaultValue = "5") int answerNumber) {
        
        try {
            log.info("AI destekli soru yükleme başlatıldı: testId={}, correctAnswer={}", testId, correctAnswer);
            
            // 1. Görseli kaydet
            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir + filename);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, file.getBytes());
            
            log.info("Görsel kaydedildi: {}", filename);
            
            // 2. AI ile analiz et
            File imageFile = new File(uploadDir + filename);
            QuestionAnalysisResult analysis = aiAnalyzer.analyzeQuestion(imageFile);
            
            log.info("AI analizi tamamlandı");
            log.debug("İpucu 1: {}", analysis.getHint1());
            log.debug("İpucu 2: {}", analysis.getHint2());
            log.debug("İpucu 3: {}", analysis.getHint3());
            log.debug("Çözüm: {}", analysis.getSolution());
            
            // 3. Question entity'si oluştur
            QuestionRequest request = new QuestionRequest();
            request.setImageUrl(filename);
            request.setCorrectAnswer(OptionLetter.valueOf(correctAnswer.toUpperCase()));
            request.setAnswerNumber(answerNumber);
            
            // 4. Question'ı kaydet
            QuestionResponse question = questionService.addQuestion(testId, request);
            
            log.info("Soru kaydedildi: questionId={}", question.getId());
            
            // 5. Hints ekle
            questionService.addHint(question.getId(), analysis.getHint1());
            questionService.addHint(question.getId(), analysis.getHint2());
            questionService.addHint(question.getId(), analysis.getHint3());
            
            log.info("3 ipucu eklendi");
            
            // 6. Explanation ekle
            questionService.addExplanation(question.getId(), analysis.getSolution());
            
            log.info("Çözüm eklendi");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Soru başarıyla yüklendi ve AI analizi tamamlandı",
                "questionId", question.getId(),
                "imageUrl", "/api/files/" + filename,
                "hints", Arrays.asList(analysis.getHint1(), analysis.getHint2(), analysis.getHint3()),
                "solution", analysis.getSolution(),
                "correctAnswer", correctAnswer,
                "answerNumber", answerNumber
            ));
            
        } catch (Exception e) {
            log.error("Soru yükleme hatası: ", e);
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "success", false,
                    "error", "Yükleme başarısız: " + e.getMessage()
                ));
        }
    }

    /**
     * Manuel soru yükleme (AI olmadan - eski yöntem)
     */
    @PostMapping("/upload-question-manual")
    public ResponseEntity<?> uploadQuestionManual(
            @RequestParam("file") MultipartFile file,
            @RequestParam("testId") Long testId,
            @RequestParam("correctAnswer") String correctAnswer,
            @RequestParam(value = "answerNumber", defaultValue = "5") int answerNumber) {
        
        try {
            log.info("Manuel soru yükleme başlatıldı: testId={}, correctAnswer={}", testId, correctAnswer);
            
            // Görseli kaydet
            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir + filename);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, file.getBytes());
            
            // Question entity'si oluştur (AI olmadan)
            QuestionRequest request = new QuestionRequest();
            request.setImageUrl(filename);
            request.setCorrectAnswer(OptionLetter.valueOf(correctAnswer.toUpperCase()));
            request.setAnswerNumber(answerNumber);
            
            // Question'ı kaydet
            QuestionResponse question = questionService.addQuestion(testId, request);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Soru başarıyla yüklendi (manuel)",
                "questionId", question.getId(),
                "imageUrl", "/api/files/" + filename,
                "note", "İpucu ve çözüm manuel olarak eklenmelidir"
            ));
            
        } catch (Exception e) {
            log.error("Manuel soru yükleme hatası: ", e);
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "success", false,
                    "error", "Yükleme başarısız: " + e.getMessage()
                ));
        }
    }
}

