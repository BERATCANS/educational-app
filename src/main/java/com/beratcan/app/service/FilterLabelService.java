package com.beratcan.app.service;

import com.beratcan.app.entity.enums.ExamType;
import com.beratcan.app.entity.enums.GradeLevel;
import com.beratcan.app.entity.enums.LessonType;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

@Service
public class FilterLabelService {

    private static final Map<GradeLevel, String> GRADE_LABELS = new EnumMap<>(GradeLevel.class);
    private static final Map<ExamType, String> EXAM_LABELS = new EnumMap<>(ExamType.class);
    private static final Map<LessonType, String> LESSON_LABELS = new EnumMap<>(LessonType.class);

    static {
        GRADE_LABELS.put(GradeLevel.G5, "5. Sınıf");
        GRADE_LABELS.put(GradeLevel.G6, "6. Sınıf");
        GRADE_LABELS.put(GradeLevel.G7, "7. Sınıf");
        GRADE_LABELS.put(GradeLevel.G8, "8. Sınıf");
        GRADE_LABELS.put(GradeLevel.G9, "9. Sınıf");
        GRADE_LABELS.put(GradeLevel.G10, "10. Sınıf");
        GRADE_LABELS.put(GradeLevel.G11, "11. Sınıf");
        GRADE_LABELS.put(GradeLevel.G12, "12. Sınıf");
        GRADE_LABELS.put(GradeLevel.MEZUN, "Mezun");

        EXAM_LABELS.put(ExamType.LGS, "LGS");
        EXAM_LABELS.put(ExamType.YKS, "YKS");
        EXAM_LABELS.put(ExamType.KPSS, "KPSS");
        EXAM_LABELS.put(ExamType.DIGER, "Diğer");

        LESSON_LABELS.put(LessonType.MATHEMATICS, "Matematik");
        LESSON_LABELS.put(LessonType.PHYSICS, "Fizik");
        LESSON_LABELS.put(LessonType.CHEMISTRY, "Kimya");
        LESSON_LABELS.put(LessonType.BIOLOGY, "Biyoloji");
        LESSON_LABELS.put(LessonType.SCIENCE, "Fen Bilimleri");
        LESSON_LABELS.put(LessonType.HISTORY, "Tarih");
        LESSON_LABELS.put(LessonType.GEOGRAPHY, "Coğrafya");
        LESSON_LABELS.put(LessonType.PHILOSOPHY, "Felsefe");
        LESSON_LABELS.put(LessonType.RELIGION, "Din Kültürü");
        LESSON_LABELS.put(LessonType.LITERATURE, "Edebiyat");
        LESSON_LABELS.put(LessonType.CIVICS, "Vatandaşlık");
        LESSON_LABELS.put(LessonType.TURKISH_HISTORY, "İnkılap Tarihi");
        LESSON_LABELS.put(LessonType.FOREIGN_LANGUAGE, "Yabancı Dil");
        LESSON_LABELS.put(LessonType.LANGUAGE, "Dil");
        LESSON_LABELS.put(LessonType.ART, "Sanat");
        LESSON_LABELS.put(LessonType.MUSIC, "Müzik");
        LESSON_LABELS.put(LessonType.OTHER, "Diğer");
    }

    public String getGradeLabel(GradeLevel gradeLevel) {
        return GRADE_LABELS.getOrDefault(gradeLevel, gradeLevel.name());
    }

    public String getExamLabel(ExamType examType) {
        return EXAM_LABELS.getOrDefault(examType, examType.name());
    }

    public String getLessonLabel(LessonType lessonType) {
        return LESSON_LABELS.getOrDefault(lessonType, lessonType.name());
    }
}
