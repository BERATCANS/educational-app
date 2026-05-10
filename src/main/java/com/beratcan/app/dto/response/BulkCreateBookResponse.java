package com.beratcan.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Bulk book create sonucunda dönen hiyerarşik ID haritası.
 * Frontend bu ID'leri kullanarak her test için bulk-import-questions çağırır.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkCreateBookResponse {

    private Long bookId;
    private String title;
    private List<TopicItem> topics;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicItem {
        private Long topicId;
        private String title;
        private Integer sequenceNumber;
        private List<TestItem> tests;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestItem {
        private Long testId;
        private String title;
        private Integer sequenceNumber;
    }
}
