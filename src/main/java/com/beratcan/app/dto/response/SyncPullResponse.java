package com.beratcan.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncPullResponse {
    private List<NoteDto> notebooks;
    private List<NotePageDto> pages;
    private List<String> deletedNotebookIds;
    private List<String> deletedPageIds;
    private long serverTime;
}
