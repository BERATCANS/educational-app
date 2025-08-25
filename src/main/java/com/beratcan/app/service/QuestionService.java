package com.beratcan.app.service;

import com.beratcan.app.dto.OptionRequest;
import com.beratcan.app.dto.QuestionPartRequest;
import com.beratcan.app.entity.*;
import com.beratcan.app.repository.QuestionRepository;
import com.beratcan.app.repository.TestRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final TestRepository testRepository;

    public QuestionService(QuestionRepository questionRepository, TestRepository testRepository) {
        this.questionRepository = questionRepository;
        this.testRepository = testRepository;
    }

    public Question addQuestion(Long testId,
                                List<QuestionPartRequest> parts, // her parça: type + content
                                List<OptionRequest> optionRequests, // artık DTO
                                OptionLetter correctAnswer,
                                int pageNumber) {

        if (parts == null || parts.isEmpty()) {
            throw new IllegalArgumentException("Question must have at least one part");
        }

        if (optionRequests == null || optionRequests.size() < 2 || optionRequests.size() > 5) {
            throw new IllegalArgumentException("Option count must be between 2 and 5");
        }

        // Testi bul
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new IllegalArgumentException("Test not found with ID: " + testId));

        // Question oluştur
        Question question = new Question();
        question.setTest(test);

        // QuestionPart’ları oluştur ve set et
        List<QuestionPart> questionParts = new ArrayList<>();
        for (QuestionPartRequest partReq : parts) {
            if (partReq.getContent() == null || partReq.getContent().trim().isEmpty()) {
                throw new IllegalArgumentException("Question part content cannot be empty");
            }
            QuestionPart part = new QuestionPart();
            part.setType(partReq.getType()); // null ise default TEXT olarak DTO’dan gelir
            part.setContent(partReq.getContent().trim());
            part.setQuestion(question);
            questionParts.add(part);
        }
        question.setParts(questionParts);

        // Option’ları oluştur
        List<Option> options = new ArrayList<>();
        for (OptionRequest optReq : optionRequests) {
            if (optReq.getContent() == null || optReq.getContent().trim().isEmpty()) {
                throw new IllegalArgumentException("Option content cannot be empty for letter " + optReq.getLetter());
            }

            Option option = new Option();
            option.setLetter(optReq.getLetter());
            option.setContent(optReq.getContent().trim());
            option.setType(optReq.getType()); // null ise DTO’dan default TEXT
            option.setQuestion(question);
            options.add(option);
        }

        // Doğru cevabı kontrol et
        boolean validCorrect = options.stream()
                .anyMatch(opt -> opt.getLetter() == correctAnswer);

        if (!validCorrect) {
            throw new IllegalArgumentException("Correct answer must match one of the provided options");
        }

        question.setOptions(options);
        question.setCorrectAnswer(correctAnswer);

        // Kaydet
        return questionRepository.save(question);
    }

    public List<Question> getQuestionsByTest(Long testId) {
        return questionRepository.findByTestId(testId);
    }
}

