package com.beratcan.app.service;

import com.beratcan.app.entity.Test;
import com.beratcan.app.entity.Topic;
import com.beratcan.app.repository.TestRepository;
import com.beratcan.app.repository.TopicRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TestService {

    private final TestRepository testRepository;
    private final TopicRepository topicRepository;

    public TestService(TestRepository testRepository, TopicRepository topicRepository) {
        this.testRepository = testRepository;
        this.topicRepository = topicRepository;
    }

    public Test addTest(Long topicId, String title) {
        Optional<Topic> topicOpt = topicRepository.findById(topicId);
        if (topicOpt.isEmpty()) throw new RuntimeException("Topic not found");

        Test test = new Test();
        test.setTitle(title);
        test.setTopic(topicOpt.get());
        return testRepository.save(test);
    }

    public List<Test> getTestsByTopic(Long topicId) {
        return testRepository.findByTopicId(topicId);
    }
}
