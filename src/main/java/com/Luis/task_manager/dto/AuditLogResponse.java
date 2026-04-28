package com.Luis.task_manager.dto;

import com.Luis.task_manager.entity.AuditLog;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogResponse {
    private Long id;
    private LocalDateTime timestamp;
    private String username;
    private String action;
    private String entityType;
    private Long entityId;
    private String details;
    private String ipAddress;
    private String userRole;

    public static AuditLogResponse from(AuditLog a) {
        AuditLogResponse r = new AuditLogResponse();
        r.id = a.getId();
        r.timestamp = a.getTimestamp();
        r.username = a.getUsername();
        r.action = a.getAction();
        r.entityType = a.getEntityType();
        r.entityId = a.getEntityId();
        r.details = a.getDetails();
        r.ipAddress = a.getIpAddress();
        r.userRole = a.getUserRole();
        return r;
    }
}
