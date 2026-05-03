package com.beratcan.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncPushResponse {

    private List<SyncResult> results;
    private long serverTime;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncResult {
        /** Frontend'in gönderdiği entity ID. */
        private String id;

        /** "ok" | "conflict" | "error" */
        private String status;

        /** Server'ın kayıttaki updatedAt değeri (frontend'in synced_at'ini buna eşitlemesi için). */
        private Long serverUpdatedAt;

        /** error/conflict durumunda açıklama. */
        private String message;
    }
}
