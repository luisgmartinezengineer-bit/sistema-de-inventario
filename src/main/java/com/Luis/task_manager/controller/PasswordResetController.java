package com.Luis.task_manager.controller;

import com.Luis.task_manager.dto.PasswordResetLogResponse;
import com.Luis.task_manager.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService resetService;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotRequest body, HttpServletRequest request) {
        try {
            String maskedEmail = resetService.requestReset(body.getUsername(), body.getEmail(), getClientIp(request));
            return ResponseEntity.ok(Map.of(
                    "message", "Correo enviado. Revisa tu bandeja de entrada.",
                    "maskedEmail", maskedEmail
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetRequest body, HttpServletRequest request) {
        try {
            resetService.resetPassword(body.getToken(), body.getNewPassword(), getClientIp(request));
            return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/password-reset-logs")
    public ResponseEntity<List<PasswordResetLogResponse>> getLogs() {
        return ResponseEntity.ok(resetService.getLogs());
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    @Data
    public static class ForgotRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String email;
    }

    @Data
    public static class ResetRequest {
        @NotBlank
        private String token;
        @NotBlank @Size(min = 6)
        private String newPassword;
    }
}
