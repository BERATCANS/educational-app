package com.beratcan.app.controller;

import com.beratcan.app.dto.request.SyncPushRequest;
import com.beratcan.app.dto.response.SyncPullResponse;
import com.beratcan.app.dto.response.SyncPushResponse;
import com.beratcan.app.service.NoteSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
public class NoteSyncController {

    private final NoteSyncService noteSyncService;

    /**
     * Delta pull. {@code since=0} ise initial sync (kullanıcının tüm aktif notları).
     */
    @GetMapping("/sync")
    @PreAuthorize("isAuthenticated()")
    public SyncPullResponse pull(
            @RequestParam(name = "since", defaultValue = "0") long since,
            Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return noteSyncService.pull(userId, since);
    }

    /**
     * Batch push. Frontend sync_queue'sundaki bekleyen kayıtları gönderir.
     */
    @PostMapping("/sync")
    @PreAuthorize("isAuthenticated()")
    public SyncPushResponse push(
            @Valid @RequestBody SyncPushRequest request,
            Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return noteSyncService.push(userId, request);
    }
}
