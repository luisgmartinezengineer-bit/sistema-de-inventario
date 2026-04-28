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

/**
 * Servicio de auditoría del sistema.
 *
 * <p>Registra todas las acciones relevantes realizadas por los usuarios
 * (creación de ventas, ajustes de stock, cambios de configuración, etc.)
 * en la tabla {@code audit_logs}.</p>
 *
 * <p>Usa propagación {@code REQUIRES_NEW} para que el registro de auditoría
 * siempre se persista en su propia transacción, independientemente de si la
 * transacción principal falla o se revierte.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Registra una entrada de auditoría con el usuario autenticado, su rol
     * y la dirección IP del cliente que originó la petición.
     *
     * @param action     nombre de la acción realizada (ej. "VENTA_CREADA")
     * @param entityType tipo de entidad afectada (ej. "Venta", "Producto")
     * @param entityId   ID de la entidad afectada
     * @param details    información adicional sobre la operación
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String entityType, Long entityId, String details) {
        String username = "system";
        String userRole = null;
        String ipAddress = null;

        // Intenta obtener el usuario autenticado del contexto de seguridad.
        // Se captura la excepción para evitar que un fallo aquí afecte la operación principal.
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

        // Intenta obtener la IP del cliente desde los atributos del request actual.
        // X-Forwarded-For se usa cuando la aplicación está detrás de un proxy o balanceador.
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
