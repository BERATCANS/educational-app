package com.beratcan.app.service;

import com.beratcan.app.dto.request.BulkCreateBookRequest;
import com.beratcan.app.dto.request.BulkImportQuestionItem;
import com.beratcan.app.dto.response.BulkCreateBookResponse;
import com.beratcan.app.dto.response.QuestionResponse;
import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.Explanation;
import com.beratcan.app.entity.Hint;
import com.beratcan.app.entity.Publisher;
import com.beratcan.app.entity.Question;
import com.beratcan.app.entity.Test;
import com.beratcan.app.entity.Topic;
import com.beratcan.app.entity.enums.AIAnalysisStatus;
import com.beratcan.app.entity.enums.ExamType;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.ConflictException;
import com.beratcan.app.exception.PublisherNotFoundException;
import com.beratcan.app.exception.TestNotFoundException;
import com.beratcan.app.repository.BookRepository;
import com.beratcan.app.repository.PublisherRepository;
import com.beratcan.app.repository.QuestionRepository;
import com.beratcan.app.repository.TestRepository;
import com.beratcan.app.storage.StoragePaths;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Toplu içe aktarma — book hiyerarşisi + AI'sız soru yükleme.
 *
 * <p>Dış AI akışından gelmiş hint/solution dolu sorular için kullanılır.
 * AI çağrısı yapılmaz, status direkt {@link AIAnalysisStatus#COMPLETED} olur.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BulkImportService {

    private static final int MAX_QUESTIONS_PER_REQUEST = 100;

    private final BookRepository bookRepository;
    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final PublisherRepository publisherRepository;
    private final FileService fileService;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final ApplicationContext applicationContext;

    /**
     * Tek istekle Book + Topics + Tests metadata oluştur.
     * Cascade=ALL sayesinde Book.save() Topic ve Test'leri de yazar.
     */
    @Transactional
    public BulkCreateBookResponse createBookHierarchy(BulkCreateBookRequest request) {
        if (request == null) {
            throw new BadRequestException("Request boş olamaz");
        }

        Publisher publisher = resolvePublisher(request.getPublisherId(), request.getPublisher());

        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setPublisher(publisher);
        book.setGradeLevel(request.getGradeLevel());
        book.setExamType(request.getExamType());
        book.setLessonType(request.getLessonType());
        book.setDescription(request.getDescription());
        book.setPaid(request.isPaid());
        book.setPrice(request.isPaid() ? request.getPrice() : BigDecimal.ZERO);

        int totalTests = 0;
        for (BulkCreateBookRequest.TopicItem topicItem : request.getTopics()) {
            Topic topic = new Topic();
            topic.setTitle(topicItem.getTitle());
            topic.setSequenceNumber(topicItem.getSequenceNumber());
            topic.setBook(book);

            int testCount = 0;
            if (topicItem.getTests() != null) {
                for (BulkCreateBookRequest.TestItem testItem : topicItem.getTests()) {
                    Test test = new Test();
                    test.setTitle(testItem.getTitle());
                    test.setSequenceNumber(testItem.getSequenceNumber());
                    test.setTopic(topic);
                    topic.getTests().add(test);
                    testCount++;
                }
            }
            topic.setNumberOfTests(testCount);
            book.getTopics().add(topic);
            totalTests += testCount;
        }

        book.setNumberOfTopics(request.getTopics().size());
        book.setNumberOfTests(totalTests);
        book.setNumberOfQuestions(0);

        Book saved = bookRepository.save(book);
        log.info("Book hiyerarşisi oluşturuldu: bookId={}, topics={}, tests={}",
                saved.getId(), saved.getTopics().size(), totalTests);

        return toBulkCreateBookResponse(saved);
    }

    /**
     * Test'in tüm sorularını + görselleri tek istekte kaydet.
     * AI çağrısı yapılmaz; hint/solution client'tan gelir, status COMPLETED.
     *
     * <p>Storage rollback'i transaction kapsamında değil — try/catch ile manuel
     * kompansasyon yapılır.
     */
    @Transactional
    public List<QuestionResponse> importQuestions(Long testId,
                                                  MultipartFile[] images,
                                                  String payloadJson) {
        if (testId == null) {
            throw new BadRequestException("testId boş olamaz");
        }
        if (images == null || images.length == 0) {
            throw new BadRequestException("En az bir görsel gönderilmelidir");
        }
        if (!StringUtils.hasText(payloadJson)) {
            throw new BadRequestException("payload boş olamaz");
        }

        List<BulkImportQuestionItem> items = parsePayload(payloadJson);
        validateItems(items, images);

        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new TestNotFoundException(testId));

        Map<String, MultipartFile> imageByName = indexImagesByFilename(images);
        ExamType examType = extractExamType(test);
        Long bookId = test.getTopic().getBook().getId();
        Long topicId = test.getTopic().getId();

        Set<Integer> requestedSequences = new HashSet<>();
        List<String> savedPaths = new ArrayList<>();
        List<QuestionResponse> responses = new ArrayList<>(items.size());

        try {
            for (int i = 0; i < items.size(); i++) {
                BulkImportQuestionItem item = items.get(i);
                MultipartFile image = imageByName.get(item.getImageFilename());

                Integer sequenceNumber = resolveSequenceNumber(testId, item.getSequenceNumber(),
                        requestedSequences);
                requestedSequences.add(sequenceNumber);

                Question question = buildQuestionEntity(test, item, sequenceNumber, examType);
                question = questionRepository.save(question); // id üret

                String ext = StoragePaths.safeExtension(image);
                String path = StoragePaths.questionImage(bookId, topicId, testId, question.getId(), ext);
                fileService.saveFile(image, path);
                savedPaths.add(path);

                question.setImageUrl(path);
                question = questionRepository.save(question);

                responses.add(toQuestionResponse(question));
                log.info("Bulk soru eklendi: questionId={}, testId={}, seq={}",
                        question.getId(), testId, sequenceNumber);
            }
        } catch (RuntimeException ex) {
            log.error("Bulk import hatası, storage compensation yapılıyor: testId={}, savedPaths={}",
                    testId, savedPaths.size(), ex);
            for (String path : savedPaths) {
                try {
                    fileService.deleteFile(path);
                } catch (Exception cleanupEx) {
                    log.warn("Storage cleanup başarısız: path={}, error={}", path, cleanupEx.getMessage());
                }
            }
            throw ex;
        }

        // Sayaçları async olarak yeniden hesapla (mevcut tek-soru akışıyla aynı örüntü)
        BookAdminService bookAdminServiceProxy = applicationContext.getBean(BookAdminService.class);
        bookAdminServiceProxy.countBookDataAsync(bookId);

        return responses;
    }

    private Publisher resolvePublisher(Long publisherId, String publisherName) {
        if (publisherId != null) {
            return publisherRepository.findById(publisherId)
                    .orElseThrow(() -> new PublisherNotFoundException(publisherId));
        }
        if (StringUtils.hasText(publisherName)) {
            String normalized = publisherName.trim();
            return publisherRepository.findByNameIgnoreCase(normalized)
                    .orElseGet(() -> {
                        Publisher created = new Publisher();
                        created.setName(normalized);
                        return publisherRepository.save(created);
                    });
        }
        throw new BadRequestException("Yayınevi bilgisi zorunludur");
    }

    private List<BulkImportQuestionItem> parsePayload(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<List<BulkImportQuestionItem>>() {});
        } catch (Exception e) {
            throw new BadRequestException("payload geçerli JSON değil: " + e.getMessage());
        }
    }

    private void validateItems(List<BulkImportQuestionItem> items, MultipartFile[] images) {
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("payload listesi boş olamaz");
        }
        if (items.size() > MAX_QUESTIONS_PER_REQUEST) {
            throw new BadRequestException("Tek istekte en fazla "
                    + MAX_QUESTIONS_PER_REQUEST + " soru eklenebilir");
        }
        if (items.size() != images.length) {
            throw new BadRequestException(
                    "payload (" + items.size() + ") ve images (" + images.length + ") sayısı eşleşmiyor");
        }

        for (int i = 0; i < items.size(); i++) {
            BulkImportQuestionItem item = items.get(i);
            Set<ConstraintViolation<BulkImportQuestionItem>> violations = validator.validate(item);
            if (!violations.isEmpty()) {
                String message = violations.iterator().next().getMessage();
                throw new BadRequestException("Soru #" + (i + 1) + " geçersiz: " + message);
            }
        }
    }

    private Map<String, MultipartFile> indexImagesByFilename(MultipartFile[] images) {
        Map<String, MultipartFile> map = new HashMap<>();
        for (MultipartFile image : images) {
            String name = image.getOriginalFilename();
            if (!StringUtils.hasText(name)) {
                throw new BadRequestException("Görsel dosyalarının orijinal adı boş olamaz");
            }
            if (map.put(name, image) != null) {
                throw new BadRequestException("Aynı dosya adına sahip birden fazla görsel: " + name);
            }
        }
        return map;
    }

    private Integer resolveSequenceNumber(Long testId, Integer requested, Set<Integer> alreadyRequested) {
        if (requested != null) {
            if (requested < 1) {
                throw new BadRequestException("Sequence number 0'dan büyük olmalıdır");
            }
            if (alreadyRequested.contains(requested)) {
                throw new BadRequestException("Sequence number " + requested
                        + " bu istekte birden fazla kez kullanılmış");
            }
            if (questionRepository.existsByTestIdAndSequenceNumber(testId, requested)) {
                throw new ConflictException("Sequence number " + requested
                        + " bu test için zaten kullanımda");
            }
            return requested;
        }
        // Otomatik: en küçük boş numara
        Integer max = questionRepository.findMaxSequenceNumberByTestId(testId);
        int candidate = (max == null) ? 1 : 1;
        while (alreadyRequested.contains(candidate)
                || questionRepository.existsByTestIdAndSequenceNumber(testId, candidate)) {
            candidate++;
        }
        return candidate;
    }

    private Question buildQuestionEntity(Test test, BulkImportQuestionItem item,
                                         Integer sequenceNumber, ExamType examType) {
        Question question = new Question();
        question.setTest(test);
        question.setImageUrl("__pending__"); // imageUrl NOT NULL — gerçek path id üretildikten sonra set edilir
        question.setCorrectAnswer(item.getCorrectAnswer());
        question.setAnswerNumber(item.getAnswerNumber());
        question.setVideoUrl(item.getVideoUrl());
        question.setSequenceNumber(sequenceNumber);
        question.setExamType(examType);
        question.setAiAnalysisStatus(AIAnalysisStatus.COMPLETED);

        question.getHints().add(buildHint(item.getHint1(), 0));
        question.getHints().add(buildHint(item.getHint2(), 1));
        question.getHints().add(buildHint(item.getHint3(), 2));

        Explanation explanation = new Explanation();
        explanation.setText(item.getSolution().trim());
        explanation.setLikes(0);
        explanation.setDislikes(0);
        question.setExplanation(explanation);

        return question;
    }

    private Hint buildHint(String text, int sequenceNumber) {
        Hint hint = new Hint();
        hint.setText(text.trim());
        hint.setSequenceNumber(sequenceNumber);
        hint.setLikes(0);
        hint.setDislikes(0);
        return hint;
    }

    private ExamType extractExamType(Test test) {
        if (test.getTopic() != null && test.getTopic().getBook() != null) {
            ExamType examType = test.getTopic().getBook().getExamType();
            if (examType != null) {
                return examType;
            }
        }
        return ExamType.DIGER;
    }

    private BulkCreateBookResponse toBulkCreateBookResponse(Book book) {
        List<BulkCreateBookResponse.TopicItem> topicItems = new ArrayList<>();
        for (Topic topic : book.getTopics()) {
            List<BulkCreateBookResponse.TestItem> testItems = new ArrayList<>();
            for (Test test : topic.getTests()) {
                testItems.add(new BulkCreateBookResponse.TestItem(
                        test.getId(), test.getTitle(), test.getSequenceNumber()));
            }
            topicItems.add(new BulkCreateBookResponse.TopicItem(
                    topic.getId(), topic.getTitle(), topic.getSequenceNumber(), testItems));
        }
        return new BulkCreateBookResponse(book.getId(), book.getTitle(), topicItems);
    }

    private QuestionResponse toQuestionResponse(Question question) {
        QuestionResponse response = new QuestionResponse();
        response.setId(question.getId());
        response.setImageUrl(question.getImageUrl());
        response.setCorrectAnswer(question.getCorrectAnswer());
        response.setAnswerNumber(question.getAnswerNumber());
        response.setVideoUrl(question.getVideoUrl());
        response.setSequenceNumber(question.getSequenceNumber());
        response.setAiAnalysisStatus(question.getAiAnalysisStatus());
        response.setExplanation(question.getExplanation() != null ? question.getExplanation().getText() : "");
        response.setHints(question.getHints().stream()
                .sorted((a, b) -> Integer.compare(a.getSequenceNumber(), b.getSequenceNumber()))
                .map(Hint::getText)
                .toList());
        return response;
    }
}
