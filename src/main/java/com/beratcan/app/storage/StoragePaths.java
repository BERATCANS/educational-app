package com.beratcan.app.storage;

import com.beratcan.app.exception.FileStorageException;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;

/**
 * Builders for ID-based hierarchical storage paths.
 *
 * <p>All paths are server-generated. Callers never embed user-supplied
 * strings in the result. A random 8-char suffix on every leaf prevents
 * URL-guessing should a path ever leak.
 *
 * <p>Path layout:
 * <ul>
 *   <li>{@code books/{bookId}/cover-{rand8}.{ext}}</li>
 *   <li>{@code books/{bookId}/topics/{topicId}/tests/{testId}/q-{questionId}-{rand8}.{ext}}</li>
 *   <li>{@code daily-questions/{dailyQuestionId}-{rand8}.{ext}}</li>
 * </ul>
 */
public final class StoragePaths {

    private static final SecureRandom RNG = new SecureRandom();
    private static final char[] ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    private StoragePaths() {
    }

    public static String bookCover(long bookId, String ext) {
        return "books/" + bookId + "/cover-" + randomSuffix() + "." + ext;
    }

    public static String questionImage(long bookId, long topicId, long testId,
                                       long questionId, String ext) {
        return "books/" + bookId
                + "/topics/" + topicId
                + "/tests/" + testId
                + "/q-" + questionId + "-" + randomSuffix() + "." + ext;
    }

    public static String dailyQuestionImage(long dailyQuestionId, String ext) {
        return "daily-questions/" + dailyQuestionId + "-" + randomSuffix() + "." + ext;
    }

    /**
     * Derive a safe extension from the upload's declared content type.
     * Filename is not consulted — clients lie. Anything outside the
     * allowlist throws {@link FileStorageException}.
     */
    public static String safeExtension(MultipartFile file) {
        String ct = file.getContentType();
        return safeExtension(ct);
    }

    public static String safeExtension(String contentType) {
        String ct = contentType == null ? "" : contentType.toLowerCase();
        return switch (ct) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            case "application/pdf" -> "pdf";
            default -> throw new FileStorageException("Desteklenmeyen dosya tipi: " + contentType);
        };
    }

    static String randomSuffix() {
        char[] out = new char[8];
        for (int i = 0; i < out.length; i++) {
            out[i] = ALPHABET[RNG.nextInt(ALPHABET.length)];
        }
        return new String(out);
    }
}
