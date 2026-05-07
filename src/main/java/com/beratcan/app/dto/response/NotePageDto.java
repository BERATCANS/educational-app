package com.beratcan.app.dto.response;

import com.beratcan.app.entity.NotePage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotePageDto {
    private String id;
    private String noteId;
    private String title;
    private String pageType;
    private int orderIndex;
    private String contentJson;
    private long createdAt;
    private long updatedAt;

    public static NotePageDto from(NotePage p) {
        return new NotePageDto(p.getId(), p.getNoteId(), p.getTitle(),
                p.getPageType().name().toLowerCase(),
                p.getOrderIndex(), p.getContentJson(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
