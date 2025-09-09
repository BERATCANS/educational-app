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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TopicService {

    private final TopicRepository topicRepository;
    private final BookRepository bookRepository;
    private final TestRepository testRepository;

    public TopicService(TopicRepository topicRepository, BookRepository bookRepository, TestRepository testRepository) {
        this.topicRepository = topicRepository;
        this.bookRepository = bookRepository;
        this.testRepository = testRepository;
    }

    // Helper: Topic -> TopicResponse
    private TopicResponse toTopicResponse(Topic topic) {
        TopicResponse response = new TopicResponse();
        response.setId(topic.getId());
        response.setTitle(topic.getTitle());
        return response;
    }

    public TopicResponse addTopic(Long bookId, String title) {
        Optional<Book> bookOpt = bookRepository.findById(bookId);
        if (bookOpt.isEmpty()) throw new RuntimeException("Book not found");

        Topic topic = new Topic();
        topic.setTitle(title);
        topic.setBook(bookOpt.get());
        Topic saved = topicRepository.save(topic);

        return toTopicResponse(saved);
    }

    public TopicResponse updateTopic(Long topicId, TopicRequest request) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic not found"));

        topic.setTitle(request.getTitle());

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
        return toTopicResponse(updated);
    }

    public void deleteTopic(Long topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic not found"));
        topicRepository.delete(topic);
    }

    public List<TopicResponse> getTopicsByBook(Long bookId) {
        return topicRepository.findByBookId(bookId)
                .stream()
                .map(this::toTopicResponse)
                .collect(Collectors.toList());
    }
}
