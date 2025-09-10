package com.beratcan.app.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class Hint {
    private String text;
    private int likes = 0;
    private int dislikes = 0;
}

