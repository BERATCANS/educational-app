package com.beratcan.app.service;

import com.beratcan.app.dto.request.TopicRequest;
import com.beratcan.app.dto.response.TopicResponse;
import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.Topic;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.BookNotFoundException;
import com.beratcan.app.exception.ConflictException;
import com.beratcan.app.exception.TopicNotFoundException;
import com.beratcan.app.repository.BookRepository;
import com.beratcan.app.repository.TopicRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final BookRepository bookRepository;
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
            if (requestedSequenceNumber < 1) {
                throw new BadRequestException("Sequence number must be greater than 0");
            }
            if (topicRepository.existsByBookIdAndSequenceNumber(bookId, requestedSequenceNumber)) {
                throw new ConflictException("Sequence number " + requestedSequenceNumber
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
        if (request == null) {
            throw new BadRequestException("Topic request cannot be null");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new BadRequestException("Topic title cannot be null or empty");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        Topic topic = new Topic();
        topic.setTitle(request.getTitle());
        topic.setBook(book);

        // Sequence number atama
        Integer sequenceNumber = getNextSequenceNumber(bookId, request.getSequenceNumber());
        topic.setSequenceNumber(sequenceNumber);

        Topic saved = topicRepository.save(topic);
        bookAdminService.countBookData(saved.getBook().getId());
        return toTopicResponse(saved);
    }

    public TopicResponse updateTopic(Long topicId, TopicRequest request) {
        if (request == null) {
            throw new BadRequestException("Topic request cannot be null");
        }

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new TopicNotFoundException(topicId));

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            topic.setTitle(request.getTitle());
        }

        Topic updated = topicRepository.save(topic);
        bookAdminService.countBookData(updated.getBook().getId());
        return toTopicResponse(updated);
    }

    public void deleteTopic(Long topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new TopicNotFoundException(topicId));
        topicRepository.delete(topic);
        bookAdminService.countBookData(topic.getBook().getId());
    }

    public List<TopicResponse> getTopicsByBook(Long bookId) {
        if (bookId == null) {
            throw new BadRequestException("Book ID cannot be null");
        }

        // Kitabın var olup olmadığını kontrol et
        bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        return topicRepository.findByBookIdOrderBySequenceNumberAsc(bookId)
                .stream()
                .map(this::toTopicResponse)
                .collect(Collectors.toList());
    }
}
