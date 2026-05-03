package com.beratcan.app.service;

import com.beratcan.app.exception.FileStorageException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageService svc;

    @BeforeEach
    void setUp() {
        svc = new LocalStorageService();
        ReflectionTestUtils.setField(svc, "uploadDir", tempDir.toString() + "/");
        ReflectionTestUtils.setField(svc, "baseUrl", "http://localhost:8080");
    }

    @Test
    void saveFile_stripsPathTraversalFromOriginalFilename() throws IOException {
        MockMultipartFile evil = new MockMultipartFile(
                "file",
                "../../etc/passwd.png",
                "image/png",
                "PNGDATA".getBytes(StandardCharsets.UTF_8));

        String stored = svc.saveFile(evil);

        Assertions.assertFalse(stored.contains(".."), "Returned filename must not contain '..'");
        Assertions.assertFalse(stored.contains("/"), "Returned filename must not contain '/'");
        Assertions.assertTrue(stored.endsWith("_passwd.png"),
                "Filename must keep only the leaf basename, prefixed with UUID");

        Path written = tempDir.resolve(stored);
        Assertions.assertTrue(Files.exists(written), "File must be written inside the temp upload dir");
        Assertions.assertArrayEquals("PNGDATA".getBytes(StandardCharsets.UTF_8),
                Files.readAllBytes(written));

        Path escapedTarget = tempDir.getParent().resolve("etc").resolve("passwd.png");
        Assertions.assertFalse(Files.exists(escapedTarget),
                "No file may have been written outside the upload root");
    }

    @Test
    void saveFile_rejectsDisallowedMime() {
        MockMultipartFile evil = new MockMultipartFile(
                "file",
                "innocent.exe",
                "application/x-msdownload",
                "MZ".getBytes(StandardCharsets.UTF_8));

        FileStorageException thrown = Assertions.assertThrows(
                FileStorageException.class,
                () -> svc.saveFile(evil));
        Assertions.assertTrue(thrown.getMessage().contains("İzin verilmeyen"));
    }

    @Test
    void saveFile_rejectsMissingMime() {
        MockMultipartFile evil = new MockMultipartFile(
                "file",
                "x.png",
                null,
                "data".getBytes(StandardCharsets.UTF_8));

        Assertions.assertThrows(FileStorageException.class, () -> svc.saveFile(evil));
    }

    @Test
    void getFile_rejectsTraversalEvenWhenTargetExists() throws IOException {
        Path outside = tempDir.getParent().resolve("outside.txt");
        Files.writeString(outside, "secret");

        FileStorageException thrown = Assertions.assertThrows(
                FileStorageException.class,
                () -> svc.getFile("../outside.txt"));
        Assertions.assertTrue(thrown.getMessage().contains("upload dizini dışında"));
    }

    @Test
    void getFile_happyPathRoundTrip() throws IOException {
        MockMultipartFile png = new MockMultipartFile(
                "file",
                "kitap.jpg",
                "image/jpeg",
                "JPEGBYTES".getBytes(StandardCharsets.UTF_8));

        String stored = svc.saveFile(png);

        Resource resource = svc.getFile(stored);
        Assertions.assertTrue(resource.exists());
        byte[] read = resource.getInputStream().readAllBytes();
        Assertions.assertArrayEquals("JPEGBYTES".getBytes(StandardCharsets.UTF_8), read);
    }

    @Test
    void saveFileBytes_rejectsPathThatEscapes() {
        FileStorageException thrown = Assertions.assertThrows(
                FileStorageException.class,
                () -> svc.saveFile("../escape.bin", new byte[]{1, 2, 3}, "application/octet-stream"));
        Assertions.assertTrue(thrown.getMessage().contains("upload dizini dışında"));
    }

    @Test
    void saveFileBytes_writesUnderRoot() throws IOException {
        String key = "audio/temp/abc.mp3";
        svc.saveFile(key, "MP3".getBytes(StandardCharsets.UTF_8), "audio/mpeg");

        Path written = tempDir.resolve(key);
        Assertions.assertTrue(Files.exists(written));
    }
}
