package com.beratcan.app.dto.response;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;


@Data
@Getter
@Setter
public class BookResponse {
    private Long id;
    private String title;
    private String publisher;
    private String coverImageUrl;
    private boolean paid;
    private int numberOfTopics;
    private int numberOfTests;
    private int numberOfQuestions;
}
