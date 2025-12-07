package com.beratcan.app.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookResponse {
    private Long id;
    private String title;
    private String publisher;
    private String coverImageUrl;
    private boolean paid;
    private Integer numberOfTopics;
    private Integer numberOfTests;
    private Integer numberOfQuestions;
}
