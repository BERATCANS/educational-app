package com.beratcan.app.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Storage service interface for file operations
 * Supports both local storage (dev) and cloud storage (prod)
 */
public interface StorageService {

    /**
     * Save a file and return the filename or URL.
     *
     * @deprecated Use {@link #saveFile(MultipartFile, String)} with a
     * server-built path from {@code StoragePaths}. This single-arg variant
     * stores into a flat namespace without ID partitioning and will be
     * removed once all callers are migrated.
     */
    @Deprecated
    String saveFile(MultipartFile file);

    /**
     * Save a multipart file under a server-supplied logical path.
     *
     * <p>The {@code logicalPath} must be built via {@code StoragePaths};
     * implementations resolve it under their storage root and reject
     * any value that escapes that root.
     *
     * @return the {@code logicalPath} (kept identical to the caller's input)
     */
    String saveFile(MultipartFile file, String logicalPath);

    /**
     * Save a file from byte array
     *
     * @param path The path/filename to save as
     * @param content The file content
     * @param contentType The content type (e.g. "audio/mpeg")
     * @return Filename (for local) or full URL (for cloud storage)
     */
    String saveFile(String path, byte[] content, String contentType);

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
     * Delete files older than specified hours with given prefix
     *
     * @param prefix The directory prefix (e.g. "audio/temp/")
     * @param hours The age threshold in hours
     */
    void deleteOldFiles(String prefix, long hours);

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
