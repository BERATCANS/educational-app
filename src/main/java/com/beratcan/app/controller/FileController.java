package com.beratcan.app.controller;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @RateLimit(requestsPerMinute = 20, keyType = "USER", message = "Too many file uploads. Please try again later.")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        String filename = fileService.saveFile(file);
        String fileUrl = fileService.getFileUrl(filename);
        log.info("📤 Dosya yüklendi - filename: {}, url: {}", filename, fileUrl);
        return ResponseEntity.ok(Map.of(
                "filename", filename,
                "url", fileUrl));
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        Resource resource = fileService.getFile(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    /**
     * Dosya silme
     */
    @DeleteMapping("/{filename}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> deleteFile(@PathVariable String filename) {
        fileService.deleteFile(filename);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Dosya başarıyla silindi"));
    }
}
