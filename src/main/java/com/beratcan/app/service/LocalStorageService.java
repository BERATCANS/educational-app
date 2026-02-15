package com.beratcan.app.service;

import com.beratcan.app.exception.FileStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@Profile({"dev", "default"}) // Sadece Development ortamında aktif
public class LocalStorageService implements StorageService {

    @Value("${file.upload.dir:uploads/}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    public String saveFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Dosya boş olamaz");
        }

        try {
            // Dosya adı oluştur
            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir + filename);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, file.getBytes());

            log.info("Dosya yüklendi (local): {}", filename);
            return filename;
        } catch (IOException e) {
            log.error("Dosya yükleme hatası: ", e);
            throw new FileStorageException("Dosya yüklenemedi: " + e.getMessage(), e);
        }
    }

    @Override
    public Resource getFile(String filename) {
        try {
            // URL'den sadece filename'i çıkar (eğer /api/files/ prefix'i varsa)
            String actualFilename = filename;
            if (filename.startsWith("/api/files/")) {
                actualFilename = filename.substring("/api/files/".length());
            } else if (filename.startsWith("api/files/")) {
                actualFilename = filename.substring("api/files/".length());
            }

            Path filePath = Paths.get(uploadDir + actualFilename);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new FileStorageException("Dosya bulunamadı veya okunamıyor: " + actualFilename);
            }

            return resource;
        } catch (IOException e) {
            log.error("Dosya okuma hatası: filename={}", filename, e);
            throw new FileStorageException("Dosya okunamadı: " + filename, e);
        }
    }

    @Override
    public void deleteFile(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new FileStorageException("Dosya adı boş olamaz");
        }

        try {
            // URL'den sadece filename'i çıkar (eğer /api/files/ prefix'i varsa)
            String actualFilename = filename;
            if (filename.startsWith("/api/files/")) {
                actualFilename = filename.substring("/api/files/".length());
            } else if (filename.startsWith("api/files/")) {
                actualFilename = filename.substring("api/files/".length());
            }

            Path filePath = Paths.get(uploadDir + actualFilename).normalize();

            // Güvenlik kontrolü: uploadDir dışına çıkmayı engelle
            Path uploadDirPath = Paths.get(uploadDir).normalize();
            if (!filePath.startsWith(uploadDirPath)) {
                throw new FileStorageException("Güvenlik hatası: Dosya upload dizini dışında");
            }

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Dosya silindi (local): {}", actualFilename);
            } else {
                log.warn("Silinecek dosya bulunamadı: {}", actualFilename);
                // Dosya yoksa exception fırlatmıyoruz, sadece log yazıyoruz
            }
        } catch (IOException e) {
            log.error("Dosya silme hatası: filename={}", filename, e);
            throw new FileStorageException("Dosya silinemedi: " + filename, e);
        }
    }

    @Override
    public String getFileUrl(String filename) {
        // Local storage için relative URL döndür
        // URL'den sadece filename'i çıkar (eğer /api/files/ prefix'i varsa)
        String actualFilename = filename;
        if (filename.startsWith("/api/files/")) {
            actualFilename = filename.substring("/api/files/".length());
        } else if (filename.startsWith("api/files/")) {
            actualFilename = filename.substring("api/files/".length());
        } else if (filename.startsWith(baseUrl + "/api/files/")) {
            actualFilename = filename.substring((baseUrl + "/api/files/").length());
        }

        return "/api/files/" + actualFilename;
    }
}
