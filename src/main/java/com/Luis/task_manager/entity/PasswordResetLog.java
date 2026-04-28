package com.Luis.task_manager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String maskedEmail;
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    private Action action;

    private LocalDateTime createdAt;

    public enum Action {
        REQUEST,    // Solicitud de recuperación enviada
        COMPLETED   // Contraseña cambiada exitosamente
    }
}
