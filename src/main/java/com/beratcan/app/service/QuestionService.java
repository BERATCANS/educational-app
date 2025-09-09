package com.beratcan.app.service;

import com.beratcan.app.dto.request.QuestionRequest;
import com.beratcan.app.dto.response.QuestionResponse;
import com.beratcan.app.entity.Question;
import com.beratcan.app.entity.Test;
import com.beratcan.app.repository.QuestionRepository;
import com.beratcan.app.repository.TestRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final TestRepository testRepository;

    public QuestionService(QuestionRepository questionRepository, TestRepository testRepository) {
        this.questionRepository = questionRepository;
        this.testRepository = testRepository;
    }

    // CREATE
    public QuestionResponse addQuestion(Long testId, QuestionRequest request) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new IllegalArgumentException("Test not found with ID: " + testId));

        Question question = new Question();
        question.setTest(test);
        question.setImageUrl(request.getImageUrl());
        question.setAnswerNumber(request.getAnswerNumber() > 0 ? request.getAnswerNumber() : 5);
        question.setCorrectAnswer(request.getCorrectAnswer());

        question = questionRepository.save(question);
        return toQuestionResponse(question);
    }

    // READ
    public List<QuestionResponse> getQuestionsByTest(Long testId) {
        return questionRepository.findByTestId(testId)
                .stream()
                .map(this::toQuestionResponse)
                .collect(Collectors.toList());
    }

    public QuestionResponse getQuestionById(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with ID: " + questionId));
        return toQuestionResponse(question);
    }

    // UPDATE
    public QuestionResponse updateQuestion(Long questionId, QuestionRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with ID: " + questionId));

        if (request.getImageUrl() != null) {
            question.setImageUrl(request.getImageUrl());
        }
        if (request.getCorrectAnswer() != null) {
            question.setCorrectAnswer(request.getCorrectAnswer());
        }
        if (request.getAnswerNumber() > 0) {
            question.setAnswerNumber(request.getAnswerNumber());
        }

        question = questionRepository.save(question);
        return toQuestionResponse(question);
    }

    // DELETE
    public void deleteQuestion(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with ID: " + questionId));
        questionRepository.delete(question);
    }

    // HELPER
    private QuestionResponse toQuestionResponse(Question question) {
        QuestionResponse response = new QuestionResponse();
        response.setId(question.getId());
        response.setImageUrl(question.getImageUrl());
        response.setCorrectAnswer(question.getCorrectAnswer());
        response.setAnswerNumber(question.getAnswerNumber());
        return response;
    }
}
