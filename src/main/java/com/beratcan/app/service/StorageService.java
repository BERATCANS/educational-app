package com.beratcan.app.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Storage service interface for file operations
 * Supports both local storage (dev) and cloud storage (prod)
 */
public interface StorageService {

    /**
     * Save a file and return the filename or URL
     * 
     * @param file The file to save
     * @return Filename (for local) or full URL (for cloud storage)
     */
    String saveFile(MultipartFile file);

    /**
     * Get a file as Resource
     * 
     * @param filename The filename or key
     * @return Resource for the file
     */
    Resource getFile(String filename);

    /**
     * Delete a file
     * 
     * @param filename The filename or key to delete
     */
    void deleteFile(String filename);

    /**
     * Get the full URL for a file
     * For local storage: returns relative URL like /api/files/{filename}
     * For cloud storage: returns full CDN URL or Spaces public URL
     * 
     * @param filename The filename or key
     * @return Full URL to access the file
     */
    String getFileUrl(String filename);
}
