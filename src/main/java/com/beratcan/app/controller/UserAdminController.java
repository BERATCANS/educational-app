package com.beratcan.app.controller;

import com.beratcan.app.dto.request.UserRequest;
import com.beratcan.app.dto.response.UserResponse;
import com.beratcan.app.entity.User;
import com.beratcan.app.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserService userService;

    /**
     * SuperAdmin: Admin veya SuperAdmin hesabı oluştur
     * POST /api/admin/users
     * Sadece SUPER_ADMIN rolüne sahip kullanıcılar erişebilir
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<UserResponse> createAdminUser(@Valid @RequestBody UserRequest request) {
        User user = userService.createUserBySuperAdmin(request);
        return ResponseEntity.ok(new UserResponse(user));
    }
}
