package com.beratcan.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class Hint {
    @Column(columnDefinition = "TEXT")
    private String text;
    private int likes = 0;
    private int dislikes = 0;
}
