package com.beratcan.app.service;

import com.beratcan.app.exception.AudioProcessingException;
import com.beratcan.app.exception.ExternalServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Speech processing servisi
 * OpenAI Whisper API (STT) ve OpenAI TTS API kullanır
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechProcessingService {
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();

    private final StorageService storageService;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.tts.model:tts-1}")
    private String ttsModel;

    @Value("${openai.tts.voice:alloy}")
    private String ttsVoice;

    @Value("${openai.tts.response.format:mp3}")
    private String ttsFormat;

    // audio.temp.dir artık StorageService tarafından yönetiliyor (prefix olarak)
    private static final String AUDIO_TEMP_PREFIX = "audio/temp/";

    /**
     * Speech-to-Text: Audio dosyasını text'e çevirir (OpenAI Whisper API)
     *
     * @param audioFile Audio dosyası (mp3, wav, webm, m4a, vb.)
     * @return Transcribed text
     */
    public String speechToText(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new AudioProcessingException("Audio dosyası boş olamaz");
        }

        log.debug("Speech-to-Text başlatıldı: filename={}, size={}KB",
                audioFile.getOriginalFilename(), audioFile.getSize() / 1024);

        try {
            // Request body oluştur (multipart/form-data)
            RequestBody fileBody = RequestBody.create(
                    audioFile.getBytes(),
                    MediaType.parse(audioFile.getContentType() != null
                            ? audioFile.getContentType()
                            : "audio/mpeg"));

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", audioFile.getOriginalFilename(), fileBody)
                    .addFormDataPart("model", "whisper-1")
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/audio/transcriptions")
                    .header("Authorization", "Bearer " + apiKey)
                    .post(requestBody)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    log.error("OpenAI Whisper API failed: status={}, error={}", response.code(), errorBody);
                    throw new ExternalServiceException(
                            String.format("OpenAI Whisper API hatası: HTTP %d - %s", response.code(), errorBody));
                }

                String responseBody = response.body().string();
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                String transcribedText = jsonResponse.get("text").asText();

                log.info("Speech-to-Text tamamlandı: textLength={} chars", transcribedText.length());
                return transcribedText;
            }

        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Speech-to-Text hatası: {}", e.getMessage(), e);
            throw new AudioProcessingException("Audio metne çevrilemedi: " + e.getMessage(), e);
        }
    }

    /**
     * Text-to-Speech: Text'i audio'ya çevirir (OpenAI TTS API)
     *
     * @param text Text to convert
     * @return Audio bytes (MP3 format)
     */
    public byte[] textToSpeech(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new AudioProcessingException("Text boş olamaz");
        }

        log.debug("Text-to-Speech başlatıldı: textLength={} chars", text.length());

        try {
            // Request body oluştur
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("model", ttsModel);
            requestMap.put("input", text);
            requestMap.put("voice", ttsVoice);
            requestMap.put("response_format", ttsFormat);
            requestMap.put("speed", 1.0);

            String requestBody = objectMapper.writeValueAsString(requestMap);

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/audio/speech")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    log.error("OpenAI TTS API failed: status={}, error={}", response.code(), errorBody);
                    throw new ExternalServiceException(
                            String.format("OpenAI TTS API hatası: HTTP %d - %s", response.code(), errorBody));
                }

                byte[] audioBytes = response.body().bytes();
                log.info("Text-to-Speech tamamlandı: audioSize={}KB", audioBytes.length / 1024);
                return audioBytes;
            }

        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Text-to-Speech hatası: {}", e.getMessage(), e);
            throw new AudioProcessingException("Text sese çevrilemedi: " + e.getMessage(), e);
        }
    }

    /**
     * Geçici audio dosyasını kaydeder
     * Temp dizine kaydeder, kalıcı storage'a konmaz
     *
     * @param audioBytes Audio byte array
     * @return Audio URL path (relative or abs depending on storage)
     */
    public String saveTemporaryAudio(byte[] audioBytes) {
        try {
            // Dosya adı oluştur
            String filename = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ".mp3";
            String path = AUDIO_TEMP_PREFIX + filename;

            // StorageService ile kaydet
            storageService.saveFile(path, audioBytes, "audio/mpeg");

            // URL döndür (StorageService'den full URL veya relative path al)
            String fileUrl = storageService.getFileUrl(path);
            
            log.debug("Geçici audio kaydedildi: path={}, url={}, size={}KB", path, fileUrl, audioBytes.length / 1024);

            return fileUrl;
        } catch (Exception e) {
            log.error("Geçici audio kaydetme hatası: {}", e.getMessage(), e);
            throw new AudioProcessingException("Audio dosyası kaydedilemedi: " + e.getMessage(), e);
        }
    }
}
