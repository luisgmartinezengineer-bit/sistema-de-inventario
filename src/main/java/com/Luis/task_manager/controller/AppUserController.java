package com.Luis.task_manager.controller;

import com.Luis.task_manager.dto.UserProfileRequest;
import com.Luis.task_manager.dto.UserProfileResponse;
import com.Luis.task_manager.dto.UserRequest;
import com.Luis.task_manager.dto.UserResponse;
import com.Luis.task_manager.service.AppUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AppUserController {

    private final AppUserService userService;

    @GetMapping
    public List<UserResponse> findAll() { return userService.findAll(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserRequest req) { return userService.create(req); }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequest req) {
        return userService.update(id, req);
    }

    @PatchMapping("/{id}/toggle")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void toggle(@PathVariable Long id) { userService.toggleActive(id); }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(userService.getProfile(principal.getUsername()));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody @Valid UserProfileRequest req,
                                           @AuthenticationPrincipal UserDetails principal) {
        try {
            return ResponseEntity.ok(userService.updateProfile(principal.getUsername(), req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<java.util.Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        try {
            userService.changePassword(principal.getUsername(), req.getCurrentPassword(), req.getNewPassword());
            return ResponseEntity.ok(java.util.Map.of("message", "Contraseña actualizada correctamente."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank private String currentPassword;
        @NotBlank @Size(min = 6) private String newPassword;
    }
}
