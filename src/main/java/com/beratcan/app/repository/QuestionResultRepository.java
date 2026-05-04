package com.beratcan.app.repository;

import com.beratcan.app.entity.QuestionResult;
import com.beratcan.app.repository.projection.StudentAccuracyProjection;
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

    /**
     * Dashboard "Hızlı İstatistikler": kullanıcının cevapladığı her sorunun
     * SON cevabı baz alınır (DISTINCT ON question_id, ORDER BY answered_at DESC).
     * Aynı soruyu birden fazla kez cevapladıysa son hâli sayılır.
     *
     * total: cevapladığı farklı soru sayısı.
     * correct: bu son cevaplardan kaçı doğru.
     *
     * Native query (PostgreSQL): DISTINCT ON ve FILTER clause JPQL'de yok.
     */
    @Query(value = """
            SELECT
              COUNT(*) AS total,
              COUNT(*) FILTER (WHERE is_correct = true) AS correct
            FROM (
              SELECT DISTINCT ON (qr.question_id) qr.is_correct
              FROM question_results qr
              JOIN test_results tr     ON qr.test_result_id = tr.id
              JOIN student_profiles sp ON tr.student_profile_id = sp.id
              WHERE sp.user_id = :userId
              ORDER BY qr.question_id, qr.answered_at DESC
            ) AS last_answers
            """, nativeQuery = true)
    StudentAccuracyProjection findStudentAccuracy(@Param("userId") Long userId);
}
