package com.beratcan.app.dto.response;

import com.beratcan.app.entity.Note;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteDto {
    private String id;
    private String title;
    private String color;
    private String icon;
    private long createdAt;
    private long updatedAt;

    public static NoteDto from(Note n) {
        return new NoteDto(n.getId(), n.getTitle(), n.getColor(), n.getIcon(),
                n.getCreatedAt(), n.getUpdatedAt());
    }
}
