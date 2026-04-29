package com.beratcan.app.storage;

import com.beratcan.app.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Streams stored files via {@link StorageService} with consistent headers.
 *
 * <p>Resource-specific download endpoints (book covers, question images,
 * daily-question images) all funnel through here so caching, content-type,
 * and inline-disposition behaviour stays uniform.
 */
@Component
@RequiredArgsConstructor
public class FileServingHelper {

    private final StorageService storageService;

    /**
     * Serve a stored file. Caller passes a server-built logical path
     * (from {@code Book.coverImageUrl} etc.) — never user input.
     *
     * @param storagePath logical path used at upload time
     * @param contentType IANA media type to set on the response
     * @param cacheVisibility {@code public} for unauthenticated content,
     *                       {@code private} for owner-gated content
     */
    public ResponseEntity<Resource> serve(String storagePath, MediaType contentType,
                                          CacheVisibility cacheVisibility) {
        if (storagePath == null || storagePath.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = storageService.getFile(storagePath);

        CacheControl cc = cacheVisibility == CacheVisibility.PUBLIC
                ? CacheControl.maxAge(Duration.ofHours(1)).cachePublic()
                : CacheControl.maxAge(Duration.ofHours(1)).cachePrivate();

        return ResponseEntity.ok()
                .contentType(contentType)
                .cacheControl(cc)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }

    public enum CacheVisibility {
        PUBLIC,
        PRIVATE
    }

    /**
     * Derive a {@link MediaType} from a stored path's extension. Falls back
     * to {@code application/octet-stream} for unknown extensions; callers
     * are responsible for refusing those at write time.
     */
    public static MediaType mediaTypeFor(String storagePath) {
        if (storagePath == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        int dot = storagePath.lastIndexOf('.');
        if (dot < 0 || dot == storagePath.length() - 1) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        String ext = storagePath.substring(dot + 1).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "webp" -> MediaType.parseMediaType("image/webp");
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "mp3" -> MediaType.parseMediaType("audio/mpeg");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
