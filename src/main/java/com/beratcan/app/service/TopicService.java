package com.beratcan.app.service;

import com.beratcan.app.dto.request.TopicRequest;
import com.beratcan.app.dto.response.TopicResponse;
import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.Test;
import com.beratcan.app.entity.Topic;
import com.beratcan.app.repository.BookRepository;
import com.beratcan.app.repository.TestRepository;
import com.beratcan.app.repository.TopicRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final BookRepository bookRepository;
    private final TestRepository testRepository;
    private final BookAdminService bookAdminService;

    // Helper: Topic -> TopicResponse
    private TopicResponse toTopicResponse(Topic topic) {
        TopicResponse response = new TopicResponse();
        response.setId(topic.getId());
        response.setTitle(topic.getTitle());
        response.setNumberOfTests(topic.getNumberOfTests());
        response.setNumberOfQuestions(topic.getNumberOfQuestions());
        response.setSequenceNumber(topic.getSequenceNumber());
        return response;
    }

    // Helper: Otomatik sequenceNumber atama
    private Integer getNextSequenceNumber(Long bookId, Integer requestedSequenceNumber) {
        if (requestedSequenceNumber != null) {
            // Eğer belirli bir sequence number isteniyorsa, o numaranın kullanılıp
            // kullanılmadığını kontrol et
            if (topicRepository.existsByBookIdAndSequenceNumber(bookId, requestedSequenceNumber)) {
                throw new RuntimeException("Sequence number " + requestedSequenceNumber
                        + " is already in use. Please delete the existing topic with this sequence number first.");
            }
            return requestedSequenceNumber;
        } else {
            // Otomatik olarak en küçük boş numarayı bul
            Integer maxSequence = topicRepository.findMaxSequenceNumberByBookId(bookId);
            if (maxSequence == null) {
                return 1; // İlk topic
            }

            // 1'den başlayarak boş numarayı bul
            for (int i = 1; i <= maxSequence + 1; i++) {
                if (!topicRepository.existsByBookIdAndSequenceNumber(bookId, i)) {
                    return i;
                }
            }
            return maxSequence + 1; // Fallback
        }
    }

    public TopicResponse addTopic(Long bookId, TopicRequest request) {
        Optional<Book> bookOpt = bookRepository.findById(bookId);
        if (bookOpt.isEmpty())
            throw new RuntimeException("Book not found");

        Topic topic = new Topic();
        topic.setTitle(request.getTitle());
        topic.setBook(bookOpt.get());

        // Sequence number atama
        Integer sequenceNumber = getNextSequenceNumber(bookId, request.getSequenceNumber());
        topic.setSequenceNumber(sequenceNumber);

        if (request.getTests() != null) {
            for (String testTitle : request.getTests()) {
                Test test = new Test();
                test.setTitle(testTitle);
                test.setTopic(topic);
                testRepository.save(test);
                topic.getTests().add(test);
            }
        }
        Topic saved = topicRepository.save(topic);
        bookAdminService.countBookData(saved.getBook().getId());
        return toTopicResponse(saved);
    }

    public TopicResponse updateTopic(Long topicId, TopicRequest request) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic not found"));

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            topic.setTitle(request.getTitle());
        }

        if (request.getTests() != null) {
            for (String testTitle : request.getTests()) {
                Test test = new Test();
                test.setTitle(testTitle);
                test.setTopic(topic);
                testRepository.save(test);
                topic.getTests().add(test);
            }
        }

        Topic updated = topicRepository.save(topic);
        bookAdminService.countBookData(updated.getBook().getId());
        return toTopicResponse(updated);
    }

    public void deleteTopic(Long topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic not found"));
        topicRepository.delete(topic);
        bookAdminService.countBookData(topic.getBook().getId());
    }

    public List<TopicResponse> getTopicsByBook(Long bookId) {
        return topicRepository.findByBookIdOrderBySequenceNumberAsc(bookId)
                .stream()
                .map(this::toTopicResponse)
                .collect(Collectors.toList());
    }
}
