package com.beratcan.app.controller;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.dto.request.LinkChildRequest;
import com.beratcan.app.dto.response.StudentSummaryDto;
import com.beratcan.app.service.ParentLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parent")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PARENT')")
public class ParentController {

    private final ParentLinkService parentLinkService;

    @GetMapping("/children")
    public ResponseEntity<List<StudentSummaryDto>> getChildren(Authentication authentication) {
        Long parentUserId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(parentLinkService.getChildren(parentUserId));
    }

    @PostMapping("/children/link")
    @RateLimit(requestsPerMinute = 10, keyType = "USER", message = "Çok fazla deneme. Daha sonra tekrar deneyin.")
    public ResponseEntity<StudentSummaryDto> linkChild(
            Authentication authentication,
            @Valid @RequestBody LinkChildRequest request) {
        Long parentUserId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(parentLinkService.linkChildByCode(parentUserId, request.getCode()));
    }

    @DeleteMapping("/children/{studentProfileId}")
    public ResponseEntity<Void> unlinkChild(
            Authentication authentication,
            @PathVariable Long studentProfileId) {
        Long parentUserId = Long.parseLong(authentication.getName());
        parentLinkService.unlinkChild(parentUserId, studentProfileId);
        return ResponseEntity.noContent().build();
    }
}
