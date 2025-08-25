package com.beratcan.app.dto;

import com.beratcan.app.entity.Option;
import com.beratcan.app.entity.OptionLetter;
import lombok.Data;

@Data
public class OptionRequest {
    private OptionLetter letter;       // A, B, C, D, E
    private String content;            // Text cevabı veya image URL
    private Option.OptionType type;    // TEXT veya IMAGE

    public Option.OptionType getType() {
        return type != null ? type : Option.OptionType.TEXT;
    }
}

