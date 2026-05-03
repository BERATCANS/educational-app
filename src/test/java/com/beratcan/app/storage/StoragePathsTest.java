package com.beratcan.app.storage;

import com.beratcan.app.exception.FileStorageException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

class StoragePathsTest {

    private static final Pattern COVER_RE =
            Pattern.compile("^books/\\d+/cover-[A-Za-z0-9]{8}\\.(jpg|png|gif|webp|pdf)$");
    private static final Pattern QUESTION_RE = Pattern.compile(
            "^books/\\d+/topics/\\d+/tests/\\d+/q-\\d+-[A-Za-z0-9]{8}\\.(jpg|png|gif|webp|pdf)$");
    private static final Pattern DAILY_RE =
            Pattern.compile("^daily-questions/\\d+-[A-Za-z0-9]{8}\\.(jpg|png|gif|webp|pdf)$");

    @Test
    void bookCover_matchesExpectedShape() {
        String p = StoragePaths.bookCover(42L, "jpg");
        Assertions.assertTrue(COVER_RE.matcher(p).matches(), "bad path: " + p);
    }

    @Test
    void questionImage_matchesExpectedShape() {
        String p = StoragePaths.questionImage(42L, 17L, 3L, 12L, "png");
        Assertions.assertTrue(QUESTION_RE.matcher(p).matches(), "bad path: " + p);
    }

    @Test
    void dailyQuestion_matchesExpectedShape() {
        String p = StoragePaths.dailyQuestionImage(7L, "webp");
        Assertions.assertTrue(DAILY_RE.matcher(p).matches(), "bad path: " + p);
    }

    @Test
    void randomSuffix_isHighlyUnlikelyToCollide() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            seen.add(StoragePaths.randomSuffix());
        }
        Assertions.assertTrue(seen.size() >= 999,
                "Random suffixes collided more than expected: " + (1000 - seen.size()));
    }

    @Test
    void safeExtension_mapsKnownTypes() {
        Assertions.assertEquals("jpg", StoragePaths.safeExtension("image/jpeg"));
        Assertions.assertEquals("jpg", StoragePaths.safeExtension("image/jpg"));
        Assertions.assertEquals("jpg", StoragePaths.safeExtension("IMAGE/JPEG"));
        Assertions.assertEquals("png", StoragePaths.safeExtension("image/png"));
        Assertions.assertEquals("gif", StoragePaths.safeExtension("image/gif"));
        Assertions.assertEquals("webp", StoragePaths.safeExtension("image/webp"));
        Assertions.assertEquals("pdf", StoragePaths.safeExtension("application/pdf"));
    }

    @Test
    void safeExtension_rejectsUnknown() {
        Assertions.assertThrows(FileStorageException.class,
                () -> StoragePaths.safeExtension("application/x-msdownload"));
        Assertions.assertThrows(FileStorageException.class,
                () -> StoragePaths.safeExtension((String) null));
        Assertions.assertThrows(FileStorageException.class,
                () -> StoragePaths.safeExtension(""));
    }

    @Test
    void safeExtension_acceptsMultipartFile() {
        MockMultipartFile f = new MockMultipartFile("file", "x.png", "image/png", new byte[]{1});
        Assertions.assertEquals("png", StoragePaths.safeExtension(f));
    }
}
