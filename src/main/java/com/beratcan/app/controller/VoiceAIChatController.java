package com.beratcan.app.controller;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.dto.response.AIMessageResponse;
import com.beratcan.app.service.VoiceAIChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.service.FileService;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Sesli AI Chat Controller
 * 
 * Text AI chat'ten tamamen ayrı.
 * Voice mesaj alır, konuşma formatında yanıt döner (LaTeX/Markdown yok).
 */
@Slf4j
@RestController
@RequestMapping("/api/questions/{questionId}/ai/voice")
@RequiredArgsConstructor
public class VoiceAIChatController {

    private final VoiceAIChatService voiceChatService;
    private final FileService fileService;

    /**
     * Voice mesaj gönder ve AI yanıtı al (Audio)
     */
    @PostMapping(value = "/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimit(requestsPerMinute = 10, keyType = "USER", message = "Çok fazla voice mesaj gönderiliyor, lütfen daha sonra tekrar deneyin.")
    public ResponseEntity<AIMessageResponse> sendVoiceMessage(
            @PathVariable Long questionId,
            @RequestParam("audioFile") MultipartFile audioFile,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        AIMessageResponse response = voiceChatService.sendVoiceMessage(questionId, userId, audioFile);
        return ResponseEntity.ok(response);
    }

    /**
     * Voice mesaj gönder ve AI yanıtı STREAM al (SSE)
     *
     * Her cümle tamamlandığında bir audio chunk gönderilir.
     */
    @PostMapping(value = "/messages/stream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimit(requestsPerMinute = 10, keyType = "USER", message = "Çok fazla voice mesaj gönderiliyor, lütfen daha sonra tekrar deneyin.")
    public SseEmitter sendVoiceMessageStream(
            @PathVariable Long questionId,
            @RequestParam("audioFile") MultipartFile audioFile,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return voiceChatService.sendVoiceMessageStream(questionId, userId, audioFile);
    }

    /**
     * Geçici audio dosyasını getir
     * Voice mesaj yanıtları için geçici audio dosyalarını serve eder
     */
    @GetMapping("/audio/temp/{filename:.+}")
    public ResponseEntity<Resource> getTemporaryAudio(
            @PathVariable String filename) {
        // Güvenlik: Path traversal önleme
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new BadRequestException("Geçersiz dosya adı");
        }

        String audioPath = "audio/temp/" + filename;
        Resource resource = fileService.getFile(audioPath);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header("Content-Disposition", "inline; filename=\"" + filename + "\"")
                .body(resource);
    }
}
