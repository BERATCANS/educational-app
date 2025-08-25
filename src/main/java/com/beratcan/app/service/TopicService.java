package com.beratcan.app.service;

import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.Topic;
import com.beratcan.app.repository.BookRepository;
import com.beratcan.app.repository.TopicRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TopicService {

    private final TopicRepository topicRepository;
    private final BookRepository bookRepository;

    public TopicService(TopicRepository topicRepository, BookRepository bookRepository) {
        this.topicRepository = topicRepository;
        this.bookRepository = bookRepository;
    }

    public Topic addTopic(Long bookId, String title) {
        Optional<Book> bookOpt = bookRepository.findById(bookId);
        if (bookOpt.isEmpty()) throw new RuntimeException("Book not found");

        Topic topic = new Topic();
        topic.setTitle(title);
        topic.setBook(bookOpt.get());
        return topicRepository.save(topic);
    }

    public List<Topic> getTopicsByBook(Long bookId) {
        return topicRepository.findByBookId(bookId);
    }
}

