package com.beratcan.app.service;

import com.beratcan.app.exception.FileStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
@RequiredArgsConstructor
public class FileService {

    @Value("${file.upload.dir:uploads/}")
    private String uploadDir;

    /**
     * Dosya yükleme
     * 
     * @param file Yüklenecek dosya
     * @return Dosya URL'si
     * @throws FileStorageException Dosya yükleme hatası
     */
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Dosya boş olamaz");
        }

        try {
            // Dosya adı oluştur
            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir + filename);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, file.getBytes());

            log.info("Dosya yüklendi: {}", filename);
            return "/api/files/" + filename;
        } catch (IOException e) {
            log.error("Dosya yükleme hatası: ", e);
            throw new FileStorageException("Dosya yüklenemedi: " + e.getMessage(), e);
        }
    }

    /**
     * Dosya okuma
     * 
     * @param filename Dosya adı
     * @return Resource
     * @throws FileStorageException Dosya bulunamadı
     */
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

    /**
     * Görsel dosyasını kaydet
     * 
     * @param file Yüklenecek dosya
     * @return Kaydedilen dosya adı (sadece filename, URL değil)
     * @throws FileStorageException Dosya kaydetme hatası
     */
    public String saveImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Dosya boş olamaz");
        }

        try {
            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir + filename);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, file.getBytes());
            return filename;
        } catch (IOException e) {
            log.error("Görsel kaydetme hatası: ", e);
            throw new FileStorageException("Görsel kaydedilemedi: " + e.getMessage(), e);
        }
    }

}
