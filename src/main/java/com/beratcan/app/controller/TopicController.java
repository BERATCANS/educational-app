package com.beratcan.app.controller;

import com.beratcan.app.dto.request.TopicRequest;
import com.beratcan.app.dto.response.TopicResponse;
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
    public TopicResponse addTopic(@RequestParam Long bookId, @RequestParam String title) {
        return topicService.addTopic(bookId, title);
    }

    @GetMapping("/by-book/{bookId}")
    public List<TopicResponse> getTopicsByBook(@PathVariable Long bookId) {
        return topicService.getTopicsByBook(bookId);
    }

    @PutMapping("/update/{topicId}")
    public TopicResponse updateTopic(@PathVariable Long topicId, @RequestBody TopicRequest request) {
        return topicService.updateTopic(topicId, request);
    }

    @DeleteMapping("/delete/{topicId}")
    public void deleteTopic(@PathVariable Long topicId) {
        topicService.deleteTopic(topicId);
    }
}