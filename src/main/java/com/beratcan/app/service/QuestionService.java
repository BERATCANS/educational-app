package com.beratcan.app.service;

import com.beratcan.app.dto.request.QuestionRequest;
import com.beratcan.app.entity.*;
import com.beratcan.app.repository.QuestionRepository;
import com.beratcan.app.repository.TestRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final TestRepository testRepository;

    public QuestionService(QuestionRepository questionRepository, TestRepository testRepository) {
        this.questionRepository = questionRepository;
        this.testRepository = testRepository;
    }

    public Question addQuestion(QuestionRequest request) {

        // Testi bul
        Test test = testRepository.findById(request.getTestId())
                .orElseThrow(() -> new IllegalArgumentException("Test not found with ID: " + request.getTestId()));

        // Question oluştur
        Question question = new Question();
        question.setTest(test);
        question.setImageUrl(request.getImageUrl());

        // Default answerNumber
        question.setAnswerNumber(request.getAnswerNumber() > 0 ? request.getAnswerNumber() : 5);

        // Doğru cevabı set et
        question.setCorrectAnswer(request.getCorrectAnswer());

        // Sıra numarası
        question.setSequenceNumber(request.getSequenceNumber());

        return questionRepository.save(question);
    }

    public List<Question> getQuestionsByTest(Long testId) {
        return questionRepository.findByTestId(testId);
    }
}

