package com.beratcan.app.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Frontend sync_queue'sundaki bekleyen kayıtları batch olarak push eder.
 * Tek transaction içinde işlenir; bir operation hata verirse o operation
 * için 'error' dönülür ama batch devam eder.
 */
@Data
public class SyncPushRequest {

    @NotEmpty(message = "operations boş olamaz")
    private List<SyncOperation> operations;

    @Data
    public static class SyncOperation {
        /** "notebook" veya "page" */
        @NotNull
        private String entityType;

        /** "upsert" veya "delete" */
        @NotNull
        private String action;

        /** Frontend'in ürettiği UUID. */
        @NotNull
        private String id;

        /**
         * upsert için entity verisi (notebook ya da page alanları).
         * delete için null olabilir.
         *
         * Notebook payload: title, color, icon, createdAt, updatedAt
         * Page payload: noteId, title, pageType, orderIndex, contentJson,
         *               thumbnailB64, createdAt, updatedAt
         */
        private Map<String, Object> payload;
    }
}
