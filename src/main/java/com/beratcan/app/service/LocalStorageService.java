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
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@Profile({"dev", "default"}) // Sadece Development ortamında aktif
public class LocalStorageService implements StorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp",
            "application/pdf",
            "audio/mpeg", "audio/mp3"
    );

    @Value("${file.upload.dir:uploads/}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    public String saveFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Dosya boş olamaz");
        }

        String contentType = file.getContentType();
        if (contentType == null
                || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new FileStorageException("İzin verilmeyen dosya tipi: " + contentType);
        }

        String original = file.getOriginalFilename();
        String basename = (original == null || original.isBlank())
                ? "file"
                : Paths.get(original).getFileName().toString();
        String filename = UUID.randomUUID() + "_" + basename;

        return writeMultipart(file, filename);
    }

    @Override
    public String saveFile(MultipartFile file, String logicalPath) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Dosya boş olamaz");
        }

        String contentType = file.getContentType();
        if (contentType == null
                || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new FileStorageException("İzin verilmeyen dosya tipi: " + contentType);
        }

        return writeMultipart(file, logicalPath);
    }

    private String writeMultipart(MultipartFile file, String logicalPath) {
        Path target = resolveSafe(logicalPath);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, file.getBytes());
            log.info("Dosya yüklendi (local): {}", logicalPath);
            return logicalPath;
        } catch (IOException e) {
            log.error("Dosya yükleme hatası: ", e);
            throw new FileStorageException("Dosya yüklenemedi: " + e.getMessage(), e);
        }
    }

    @Override
    public Resource getFile(String filename) {
        try {
            Path target = resolveSafe(filename);
            Resource resource = new UrlResource(target.toUri());

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
            Path target = resolveSafe(filename);

            if (Files.exists(target)) {
                Files.delete(target);
                log.info("Dosya silindi (local): {}", filename);
            } else {
                log.warn("Silinecek dosya bulunamadı: {}", filename);
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

        Path target = resolveSafe(path);

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);

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
            Path dirPath = resolveSafe(prefix);
            if (!Files.exists(dirPath)) {
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

    /**
     * Resolves the requested relative path against the upload root and refuses
     * anything that escapes it. Centralizes the boundary check so callers
     * (and future variants) cannot forget it.
     */
    private Path resolveSafe(String relative) {
        if (relative == null) {
            throw new FileStorageException("Yol boş olamaz");
        }
        Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path resolved = uploadRoot.resolve(relative).normalize();
        if (!resolved.startsWith(uploadRoot)) {
            throw new FileStorageException("Güvenlik hatası: yol upload dizini dışında");
        }
        return resolved;
    }
}
