package com.beratcan.app.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class FlaggedResponse {
    private Long questionId;
    private String imageUrl;
    private int answerNumber;
    private String correctAnswer;
    private String videoUrl;
    private String explanation;
    private List<String> hints;

    // Navigation için gerekli ID'ler
    private Long bookId;
    private Long topicId;
    private Long testId;

    // Display için ek bilgiler
    private String bookTitle;
    private String topicTitle;
    private String testTitle;
}
