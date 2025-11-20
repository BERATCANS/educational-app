package com.beratcan.app.service;

import com.beratcan.app.dto.response.FlaggedResponse;
import com.beratcan.app.entity.Question;
import com.beratcan.app.exception.ConflictException;
import com.beratcan.app.exception.QuestionNotFoundException;
import com.beratcan.app.exception.UserNotFoundException;
import com.beratcan.app.repository.QuestionRepository;
import com.beratcan.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionFlagService {

    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;

    @Transactional
    public void flagQuestion(Long userId, Long questionId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        var question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        if (user.getFlaggedQuestions().contains(question)) {
            throw new ConflictException("Question is already flagged");
        }

        user.getFlaggedQuestions().add(question);
        userRepository.save(user);

        log.info("Question flagged: userId={}, questionId={}, totalFlagged={}",
                userId, questionId, user.getFlaggedQuestions().size());
    }

    @Transactional
    public void unflagQuestion(Long userId, Long questionId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        var question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        if (!user.getFlaggedQuestions().contains(question)) {
            throw new ConflictException("Question is not flagged");
        }

        user.getFlaggedQuestions().remove(question);
        userRepository.save(user);

        log.info("Question unflagged: userId={}, questionId={}, totalFlagged={}",
                userId, questionId, user.getFlaggedQuestions().size());
    }

    public List<FlaggedResponse> getFlaggedQuestions(Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Set<Question> flaggedQuestions = user.getFlaggedQuestions();
        return flaggedQuestions.stream()
                .map(this::toFlaggedResponse)
                .toList();
    }

    public FlaggedResponse getFlaggedQuestionById(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        return toFlaggedResponse(question);
    }

    private FlaggedResponse toFlaggedResponse(Question question) {
        FlaggedResponse fr = new FlaggedResponse();
        fr.setQuestionId(question.getId());
        fr.setImageUrl(question.getImageUrl());
        fr.setAnswerNumber(question.getAnswerNumber());
        fr.setCorrectAnswer(question.getCorrectAnswer().name());
        fr.setVideoUrl(question.getVideoUrl());

        // Hints ve explanation
        fr.setHints(question.getHints() != null
                ? question.getHints().stream().map(hint -> hint.getText()).toList()
                : List.of());
        fr.setExplanation(question.getExplanation() != null ? question.getExplanation().getText() : "");

        // Navigation ve display bilgileri
        if (question.getTest() != null) {
            fr.setTestId(question.getTest().getId());
            fr.setTestTitle(question.getTest().getTitle());

            if (question.getTest().getTopic() != null) {
                fr.setTopicId(question.getTest().getTopic().getId());
                fr.setTopicTitle(question.getTest().getTopic().getTitle());

                if (question.getTest().getTopic().getBook() != null) {
                    fr.setBookId(question.getTest().getTopic().getBook().getId());
                    fr.setBookTitle(question.getTest().getTopic().getBook().getTitle());
                }
            }
        }

        return fr;
    }
}
