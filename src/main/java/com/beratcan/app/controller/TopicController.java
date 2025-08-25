package com.beratcan.app.controller;

import com.beratcan.app.entity.Topic;
import com.beratcan.app.service.TopicService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @PostMapping("/add")
    public Topic addTopic(@RequestParam Long bookId, @RequestParam String title) {
        return topicService.addTopic(bookId, title);
    }

    @GetMapping("/by-book/{bookId}")
    public List<Topic> getTopicsByBook(@PathVariable Long bookId) {
        return topicService.getTopicsByBook(bookId);
    }
}
