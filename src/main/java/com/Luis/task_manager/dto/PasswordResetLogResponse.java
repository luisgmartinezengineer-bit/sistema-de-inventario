package com.Luis.task_manager.dto;

import com.Luis.task_manager.entity.PasswordResetLog;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PasswordResetLogResponse {
    private Long id;
    private String username;
    private String maskedEmail;
    private String ipAddress;
    private String action;
    private String actionLabel;
    private LocalDateTime createdAt;

    public static PasswordResetLogResponse from(PasswordResetLog log) {
        PasswordResetLogResponse r = new PasswordResetLogResponse();
        r.id = log.getId();
        r.username = log.getUsername();
        r.maskedEmail = log.getMaskedEmail();
        r.ipAddress = log.getIpAddress();
        r.action = log.getAction() != null ? log.getAction().name() : null;
        r.actionLabel = log.getAction() == PasswordResetLog.Action.REQUEST
                ? "Solicitud enviada"
                : "Contraseña cambiada";
        r.createdAt = log.getCreatedAt();
        return r;
    }
}
