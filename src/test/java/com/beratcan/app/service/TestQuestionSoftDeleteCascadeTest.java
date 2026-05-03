package com.beratcan.app.service;

import com.beratcan.app.entity.Question;
import com.beratcan.app.entity.Test;
import com.beratcan.app.entity.Topic;
import com.beratcan.app.entity.enums.OptionLetter;
import com.beratcan.app.repository.QuestionRepository;
import com.beratcan.app.repository.TestRepository;
import com.beratcan.app.repository.TopicRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@Transactional
public class TestQuestionSoftDeleteCascadeTest {

    @Autowired private TestService testService;
    @Autowired private TestRepository testRepository;
    @Autowired private TopicRepository topicRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager em;

    private Topic topic;

    @BeforeEach
    void setup() {
        topic = topicRepository.findById(5L).orElseThrow(
                () -> new IllegalStateException("Seed topic id=5 not found"));
    }

    @org.junit.jupiter.api.Test
    void deleteTest_softDeletesParentAndAllChildQuestions() {
        Test t = newTestWithSequence(99001);
        t = testRepository.saveAndFlush(t);

        List<Long> questionIds = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Question q = newQuestion(t, i);
            q = questionRepository.saveAndFlush(q);
            questionIds.add(q.getId());
        }

        Long testId = t.getId();

        testService.deleteTest(testId);
        em.flush();
        em.clear();

        Boolean testDeleted = jdbc.queryForObject(
                "SELECT is_deleted FROM tests WHERE id = ?", Boolean.class, testId);
        Assertions.assertEquals(Boolean.TRUE, testDeleted, "Test must be soft-deleted");

        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM questions WHERE test_id = ?", Integer.class, testId);
        Assertions.assertEquals(3, total, "Question rows must still exist physically");

        Integer alive = jdbc.queryForObject(
                "SELECT COUNT(*) FROM questions WHERE test_id = ? AND is_deleted = false",
                Integer.class, testId);
        Assertions.assertEquals(0, alive, "All child questions must be soft-deleted");
    }

    @org.junit.jupiter.api.Test
    void softDeleteByTestId_skipsAlreadyDeletedQuestions() {
        Test t = newTestWithSequence(99002);
        t = testRepository.saveAndFlush(t);
        Long testId = t.getId();

        Question q1 = questionRepository.saveAndFlush(newQuestion(t, 1));
        questionRepository.saveAndFlush(newQuestion(t, 2));
        questionRepository.saveAndFlush(newQuestion(t, 3));

        questionRepository.delete(q1);
        em.flush();

        int affected = questionRepository.softDeleteByTestId(testId);
        em.flush();
        em.clear();

        Assertions.assertEquals(2, affected,
                "Bulk update must skip the already-soft-deleted question");

        Integer alive = jdbc.queryForObject(
                "SELECT COUNT(*) FROM questions WHERE test_id = ? AND is_deleted = false",
                Integer.class, testId);
        Assertions.assertEquals(0, alive);

        Integer deleted = jdbc.queryForObject(
                "SELECT COUNT(*) FROM questions WHERE test_id = ? AND is_deleted = true",
                Integer.class, testId);
        Assertions.assertEquals(3, deleted);
    }

    private Test newTestWithSequence(int seq) {
        Test t = new Test();
        t.setTitle("cascade-test-" + seq);
        t.setTopic(topic);
        t.setSequenceNumber(seq);
        t.setNumberOfQuestions(0);
        return t;
    }

    private Question newQuestion(Test t, int seq) {
        Question q = new Question();
        q.setTest(t);
        q.setImageUrl("https://example.invalid/q-" + seq + ".png");
        q.setAnswerNumber(5);
        q.setCorrectAnswer(OptionLetter.A);
        q.setSequenceNumber(seq);
        return q;
    }
}
