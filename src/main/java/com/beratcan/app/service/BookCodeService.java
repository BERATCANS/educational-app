package com.beratcan.app.service;

import com.beratcan.app.dto.request.UpdateBookCodeRequest;
import com.beratcan.app.dto.response.BookCodeResponse;
import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.BookCode;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.BookCodeNotFoundException;
import com.beratcan.app.exception.BookNotFoundException;
import com.beratcan.app.repository.BookCodeRepository;
import com.beratcan.app.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookCodeService {

    private final BookCodeRepository bookCodeRepository;
    private final BookRepository bookRepository;

    /**
     * Tüm book code'ları getir
     */
    @Transactional(readOnly = true)
    public List<BookCodeResponse> getAllBookCodes() {
        log.info("Tüm book code'lar getiriliyor");
        // JOIN FETCH ile book ve usedBy ilişkilerini tek query'de yükle
        List<BookCode> codes = bookCodeRepository.findAllWithBookAndUsedBy();
        return codes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Belirli bir kitaba ait tüm code'ları getir
     */
    @Transactional(readOnly = true)
    public List<BookCodeResponse> getBookCodesByBookId(Long bookId) {
        log.info("Book code'lar getiriliyor: bookId={}", bookId);

        // Book'un var olup olmadığını kontrol et
        if (!bookRepository.existsById(bookId)) {
            throw new BookNotFoundException(bookId);
        }

        // JOIN FETCH ile book ve usedBy ilişkilerini tek query'de yükle
        List<BookCode> codes = bookCodeRepository.findByBookIdWithBookAndUsedBy(bookId);
        return codes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Book code güncelle
     */
    @Transactional
    public BookCodeResponse updateBookCode(Long codeId, UpdateBookCodeRequest request) {
        log.info("Book code güncelleniyor: codeId={}", codeId);

        // JOIN FETCH ile book ve usedBy ilişkilerini tek query'de yükle
        BookCode bookCode = bookCodeRepository.findByIdWithBookAndUsedBy(codeId)
                .orElseThrow(() -> new BookCodeNotFoundException(codeId));

        // Yeni kod zaten kullanılıyorsa ve farklı bir code ise hata ver
        if (!bookCode.getCode().equals(request.getCode()) &&
                bookCodeRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Bu kod zaten kullanılıyor: " + request.getCode());
        }

        bookCode.setCode(request.getCode());
        BookCode saved = bookCodeRepository.save(bookCode);
        log.info("Book code güncellendi: codeId={}, newCode={}", codeId, request.getCode());

        return mapToResponse(saved);
    }

    /**
     * Book code sil
     */
    @Transactional
    public void deleteBookCode(Long codeId) {
        log.info("Book code siliniyor: codeId={}", codeId);

        BookCode bookCode = bookCodeRepository.findById(codeId)
                .orElseThrow(() -> new BookCodeNotFoundException(codeId));

        // Eğer kod kullanılmışsa silme işlemini engelle
        if (bookCode.isUsed()) {
            throw new BadRequestException("Kullanılmış kod silinemez: " + bookCode.getCode());
        }

        bookCodeRepository.delete(bookCode);
        log.info("Book code silindi: codeId={}", codeId);
    }

    /**
     * Bir kitaba birden fazla kod ekle
     */
    @Transactional
    public String addCodes(Long bookId, List<String> codes) {
        log.info("Book code'lar ekleniyor: bookId={}, codeCount={}", bookId, codes != null ? codes.size() : 0);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        if (codes == null || codes.isEmpty()) {
            throw new BadRequestException("Eklenebilecek kod bulunamadı.");
        }

        // Duplicate ve yeni kodları ayır
        List<String> duplicateCodes = codes.stream()
                .filter(codeStr -> bookCodeRepository.existsByCode(codeStr))
                .toList();

        List<String> newCodes = codes.stream()
                .filter(codeStr -> !bookCodeRepository.existsByCode(codeStr))
                .toList();

        // Yeni kodları ekle
        if (!newCodes.isEmpty()) {
            List<BookCode> bookCodes = newCodes.stream().map(codeStr -> {
                BookCode code = new BookCode();
                code.setCode(codeStr);
                code.setBook(book);
                code.setUsed(false);
                return code;
            }).toList();

            book.getCodes().addAll(bookCodes);
            bookCodeRepository.saveAll(bookCodes);
            log.info("{} kod başarıyla eklendi: bookId={}", newCodes.size(), bookId);
        }

        // Sonuç mesajı oluştur
        StringBuilder result = new StringBuilder();

        if (!newCodes.isEmpty()) {
            result.append(newCodes.size()).append(" kod başarıyla eklendi");
            if (!duplicateCodes.isEmpty()) {
                result.append(". ");
            }
        }

        if (!duplicateCodes.isEmpty()) {
            if (newCodes.isEmpty()) {
                result.append("Hiçbir kod eklenemedi. ");
            }
            result.append("Bu kodlar zaten mevcut: ").append(String.join(", ", duplicateCodes));
        }

        if (newCodes.isEmpty() && duplicateCodes.isEmpty()) {
            result.append("Hiçbir kod belirtilmedi");
        }

        return result.toString();
    }

    /**
     * BookCode entity'sini BookCodeResponse DTO'suna map et
     */
    private BookCodeResponse mapToResponse(BookCode bookCode) {
        BookCodeResponse response = new BookCodeResponse();
        response.setId(bookCode.getId());
        response.setCode(bookCode.getCode());
        response.setUsed(bookCode.isUsed());
        response.setBookId(bookCode.getBook().getId());
        response.setBookTitle(bookCode.getBook().getTitle());

        if (bookCode.getUsedBy() != null) {
            response.setUsedByUserId(bookCode.getUsedBy().getId());
            response.setUsedByUsername(bookCode.getUsedBy().getUsername());
        }

        return response;
    }
}
