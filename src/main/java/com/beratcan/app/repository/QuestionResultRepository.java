package com.beratcan.app.repository;

import com.beratcan.app.entity.QuestionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionResultRepository extends JpaRepository<QuestionResult, Long> {

    // Test sonucu için soru cevapları
    List<QuestionResult> findByTestResultId(Long testResultId);

    // Kullanıcının belirli bir soru için tüm cevapları
    @Query("SELECT qr FROM QuestionResult qr JOIN qr.testResult tr WHERE tr.studentProfile.user.id = :userId AND qr.question.id = :questionId ORDER BY qr.answeredAt DESC")
    List<QuestionResult> findByUserIdAndQuestionId(@Param("userId") Long userId, @Param("questionId") Long questionId);

    // En çok yanlış yapılan sorular
    @Query("SELECT qr.question.id, COUNT(qr) as wrongCount FROM QuestionResult qr WHERE qr.isCorrect = false GROUP BY qr.question.id ORDER BY wrongCount DESC")
    List<Object[]> findMostWrongQuestions();

    // Kullanıcının en çok zorlandığı sorular
    @Query("SELECT qr.question.id, COUNT(qr) as wrongCount FROM QuestionResult qr JOIN qr.testResult tr WHERE tr.studentProfile.user.id = :userId AND qr.isCorrect = false GROUP BY qr.question.id ORDER BY wrongCount DESC")
    List<Object[]> findUserMostWrongQuestions(@Param("userId") Long userId);
}
