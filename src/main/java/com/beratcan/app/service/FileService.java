package com.beratcan.app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * FileService - Wrapper for StorageService
 * This class provides backward compatibility for existing code
 * while delegating to the appropriate StorageService implementation
 * (LocalStorageService for dev, CloudStorageService for prod)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final StorageService storageService;

    /**
     * Dosya kaydetme
     *
     * @param file Yüklenecek dosya
     * @return Kaydedilen dosya adı (sadece filename, URL değil)
     */
    public String saveFile(MultipartFile file) {
        return storageService.saveFile(file);
    }

    /**
     * Save a multipart file under a server-supplied logical path.
     * Use {@link com.beratcan.app.storage.StoragePaths} to build the path.
     */
    public String saveFile(MultipartFile file, String logicalPath) {
        return storageService.saveFile(file, logicalPath);
    }

    /**
     * Dosya okuma
     * 
     * @param filename Dosya adı
     * @return Resource
     */
    public Resource getFile(String filename) {
        return storageService.getFile(filename);
    }

    /**
     * Dosya silme
     * 
     * @param filename Silinecek dosya adı
     */
    public void deleteFile(String filename) {
        storageService.deleteFile(filename);
            }

    /**
     * Get file URL (for cloud storage compatibility)
     * 
     * @param filename Dosya adı
     * @return Full URL or relative URL
     */
    public String getFileUrl(String filename) {
        return storageService.getFileUrl(filename);
        }
}
