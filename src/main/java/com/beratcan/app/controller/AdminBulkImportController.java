package com.beratcan.app.controller;

import com.beratcan.app.dto.request.BulkCreateBookRequest;
import com.beratcan.app.dto.response.BulkCreateBookResponse;
import com.beratcan.app.dto.response.QuestionResponse;
import com.beratcan.app.service.BulkImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Admin-only toplu içe aktarma endpoint'leri.
 * Rate limit yok — yığın halinde içerik yüklemek içindir.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminBulkImportController {

    private final BulkImportService bulkImportService;

    /**
     * Tek istekle Book + Topics + Tests metadata oluştur.
     * Görsel yok — book cover ayrı endpoint ile yüklenir.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/books/bulk-create")
    public BulkCreateBookResponse createBookHierarchy(@Valid @RequestBody BulkCreateBookRequest request) {
        return bulkImportService.createBookHierarchy(request);
    }

    /**
     * Bir test için tüm soruları + görselleri tek istekte yükle.
     * AI çağrısı atlanır; hint/solution payload'dan gelir.
     *
     * @param testId      hangi teste eklenecek
     * @param images      soru görselleri (her birinin orijinal adı payload'daki imageFilename ile eşleşir)
     * @param payload     JSON string — List&lt;BulkImportQuestionItem&gt;
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping(value = "/questions/bulk-import", consumes = "multipart/form-data")
    public List<QuestionResponse> bulkImportQuestions(
            @RequestParam("testId") Long testId,
            @RequestParam("images") MultipartFile[] images,
            @RequestParam("payload") String payload) {
        return bulkImportService.importQuestions(testId, images, payload);
    }
}
