package com.Luis.task_manager.controller;

import com.Luis.task_manager.dto.AuditLogResponse;
import com.Luis.task_manager.repository.AuditLogRepository;
import com.Luis.task_manager.service.AuditExcelExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final AuditExcelExportService excelExportService;

    @GetMapping
    public List<AuditLogResponse> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        String u = (username  != null && !username.isBlank())  ? username  : null;
        String e = (entityType!= null && !entityType.isBlank())? entityType: null;
        String a = (action    != null && !action.isBlank())    ? action    : null;
        LocalDateTime fromDt = from != null ? from.atStartOfDay()              : null;
        LocalDateTime toDt   = to   != null ? to.atTime(23, 59, 59) : null;

        return auditLogRepository.findWithFilters(u, e, a, fromDt, toDt, PageRequest.of(page, size))
                .stream().map(AuditLogResponse::from).collect(Collectors.toList());
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        LocalDateTime now   = LocalDateTime.now();
        LocalDateTime today = now.toLocalDate().atStartOfDay();
        LocalDateTime week  = now.minusDays(7);
        LocalDateTime month = now.minusDays(30);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("today",       auditLogRepository.countSince(today));
        result.put("week",        auditLogRepository.countSince(week));
        result.put("month",       auditLogRepository.countSince(month));
        result.put("total",       auditLogRepository.count());

        Map<String, Long> byAction = new LinkedHashMap<>();
        for (Object[] row : auditLogRepository.countByAction())
            byAction.put((String) row[0], (Long) row[1]);
        result.put("byAction", byAction);

        Map<String, Long> byEntity = new LinkedHashMap<>();
        for (Object[] row : auditLogRepository.countByEntityType())
            byEntity.put((String) row[0], (Long) row[1]);
        result.put("byEntity", byEntity);

        List<Map<String, Object>> topUsers = auditLogRepository.topUsersSince(week).stream()
                .limit(5)
                .map(row -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("username", row[0]);
                    m.put("count",    row[1]);
                    return m;
                }).collect(Collectors.toList());
        result.put("topUsers", topUsers);

        return result;
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            byte[] bytes = excelExportService.generate(username, entityType, action, from, to);
            String filename = "Auditoria_" + java.time.LocalDate.now() + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception ex) {
            log.error("Error al generar exportación de auditoría", ex);
            return ResponseEntity.internalServerError().build();
        }
    }
}
