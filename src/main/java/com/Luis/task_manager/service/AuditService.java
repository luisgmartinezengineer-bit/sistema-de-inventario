package com.Luis.task_manager.service;

import com.Luis.task_manager.entity.AppUser;
import com.Luis.task_manager.entity.AuditLog;
import com.Luis.task_manager.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String entityType, Long entityId, String details) {
        String username = "system";
        String userRole = null;
        String ipAddress = null;

        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                username = auth.getName();
                if (auth.getPrincipal() instanceof AppUser u) {
                    userRole = u.getRole() != null ? u.getRole().name() : null;
                }
            }
        } catch (Exception e) {
            log.debug("No se pudo obtener el usuario del contexto de seguridad", e);
        }

        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String forwarded = req.getHeader("X-Forwarded-For");
                ipAddress = (forwarded != null && !forwarded.isBlank())
                        ? forwarded.split(",")[0].trim()
                        : req.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("No se pudo obtener la IP del request actual", e);
        }

        auditLogRepository.save(AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .username(username)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .ipAddress(ipAddress)
                .userRole(userRole)
                .build());
    }
}
