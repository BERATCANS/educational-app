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
            Path filePath = Paths.get(uploadDir + filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new FileStorageException("Dosya bulunamadı veya okunamıyor: " + filename);
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
            Path filePath = Paths.get(uploadDir + filename).normalize();

            // Güvenlik kontrolü: uploadDir dışına çıkmayı engelle
            Path uploadDirPath = Paths.get(uploadDir).normalize();
            if (!filePath.startsWith(uploadDirPath)) {
                throw new FileStorageException("Güvenlik hatası: Dosya upload dizini dışında");
            }

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Dosya silindi (local): {}", filename);
            } else {
                log.warn("Silinecek dosya bulunamadı: {}", filename);
                // Dosya yoksa exception fırlatmıyoruz, sadece log yazıyoruz
            }
        } catch (IOException e) {
            log.error("Dosya silme hatası: filename={}", filename, e);
            throw new FileStorageException("Dosya silinemedi: " + filename, e);
        }
    }

    @Override
    public String getFileUrl(String filename) {
        return filename;
    }

    @Override
    public String saveFile(String path, byte[] content, String contentType) {
        if (content == null || content.length == 0) {
            throw new FileStorageException("Dosya içeriği boş olamaz");
        }

        try {
            Path filePath = Paths.get(uploadDir + path).normalize();
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, content);

            log.info("Dosya kaydedildi (local): {}", path);
            return path;
        } catch (IOException e) {
            log.error("Dosya kaydetme hatası: ", e);
            throw new FileStorageException("Dosya kaydedilemedi: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteOldFiles(String prefix, long hours) {
        try {
            Path dirPath = Paths.get(uploadDir + prefix).normalize();
            if (!Files.exists(dirPath)) {
                return;
            }

            // Güvenlik: Sadece uploadDir altındaki prefix'leri kabul et
            if (!dirPath.startsWith(Paths.get(uploadDir).normalize())) {
                log.warn("Güvenlik uyarısı: Upload dizini dışı temizlik denemesi: {}", prefix);
                return;
            }

            long now = System.currentTimeMillis();
            long maxAgeMillis = hours * 60 * 60 * 1000;

            Files.walk(dirPath)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            long lastModified = Files.getLastModifiedTime(file).toMillis();
                            if (now - lastModified > maxAgeMillis) {
                                Files.delete(file);
                                log.info("Eski dosya silindi (local): {}", file.getFileName());
                            }
                        } catch (IOException e) {
                            log.error("Dosya silinemedi: {}", file, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Eski dosyaları temizleme hatası: ", e);
        }
    }
}
