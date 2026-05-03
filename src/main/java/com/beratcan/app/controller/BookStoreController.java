package com.beratcan.app.controller;

import com.beratcan.app.constants.AppConstants;
import com.beratcan.app.dto.response.BookDetailResponse;
import com.beratcan.app.dto.response.BookStoreResponse;
import com.beratcan.app.dto.response.FilterOptionResponse;
import com.beratcan.app.dto.response.PageResponse;
import com.beratcan.app.entity.enums.ExamType;
import com.beratcan.app.entity.enums.GradeLevel;
import com.beratcan.app.entity.enums.LessonType;
import com.beratcan.app.entity.Book;
import com.beratcan.app.exception.BookNotFoundException;
import com.beratcan.app.repository.BookRepository;
import com.beratcan.app.service.BookCatalogService;
import com.beratcan.app.service.FilterLabelService;
import com.beratcan.app.storage.FileServingHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookStoreController {

        private final BookCatalogService bookCatalogService;
        private final FilterLabelService filterLabelService;
        private final BookRepository bookRepository;
        private final FileServingHelper fileServingHelper;

        @GetMapping("/store")
        public PageResponse<BookStoreResponse> getBookStore(
                        @RequestParam(required = false) String query,
                        @RequestParam(required = false) List<ExamType> examTypes,
                        @RequestParam(required = false) List<GradeLevel> gradeLevels,
                        @RequestParam(required = false) List<LessonType> lessonTypes,
                        @RequestParam(required = false) BigDecimal minPrice,
                        @RequestParam(required = false) BigDecimal maxPrice,
                        Authentication authentication,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "" + AppConstants.DEFAULT_PAGE_SIZE) int size) {
                Long userId = (authentication != null && authentication.isAuthenticated())
                                ? Long.parseLong(authentication.getName())
                                : null;
                return bookCatalogService.getBookStore(
                                query, examTypes, gradeLevels, lessonTypes, minPrice, maxPrice, userId, page, size);
        }

        @GetMapping("/{bookId}")
        public ResponseEntity<BookDetailResponse> getBookDetail(
                        @PathVariable Long bookId,
                        Authentication authentication) {
                Long userId = (authentication != null && authentication.isAuthenticated())
                                ? Long.parseLong(authentication.getName())
                                : null;
                BookDetailResponse response = bookCatalogService.getBookDetail(bookId, userId);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/{bookId}/cover")
        public ResponseEntity<Resource> getCover(@PathVariable Long bookId) {
                Book book = bookRepository.findById(bookId)
                                .orElseThrow(() -> new BookNotFoundException(bookId));
                return fileServingHelper.serve(
                                book.getCoverImageUrl(),
                                FileServingHelper.mediaTypeFor(book.getCoverImageUrl()),
                                FileServingHelper.CacheVisibility.PUBLIC);
        }

        /**
         * Filter options endpoint
         * Static enum data'yı cache'ler, her çağrıda yeniden hesaplamaz
         */
        @GetMapping("/filters")
        @Cacheable(value = "filterOptions", key = "'all'")
        public Map<String, List<FilterOptionResponse>> getBookFilters() {
                Map<String, List<FilterOptionResponse>> filters = new HashMap<>();
                filters.put("gradeLevels",
                                Arrays.stream(GradeLevel.values())
                                                .map(level -> new FilterOptionResponse(level.name(),
                                                                filterLabelService.getGradeLabel(level)))
                                                .toList());
                filters.put("examTypes",
                                Arrays.stream(ExamType.values())
                                                .map(exam -> new FilterOptionResponse(exam.name(),
                                                                filterLabelService.getExamLabel(exam)))
                                                .toList());
                filters.put("lessonTypes",
                                Arrays.stream(LessonType.values())
                                                .map(lesson -> new FilterOptionResponse(lesson.name(),
                                                                filterLabelService.getLessonLabel(lesson)))
                                                .toList());
                return filters;
        }
}
