package com.Luis.task_manager.dto;

import com.Luis.task_manager.entity.SupplierAlert;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SupplierAlertResponse {
    private Long id;
    private Long supplierProductId;
    private String supplierName;
    private String productName;
    private String type;
    private String message;
    private BigDecimal referenceValue;
    private boolean resolved;
    private LocalDateTime createdAt;
    private LocalDate estimatedEventDate;

    public static SupplierAlertResponse from(SupplierAlert a) {
        SupplierAlertResponse r = new SupplierAlertResponse();
        r.id = a.getId();
        r.supplierProductId = a.getSupplierProduct().getId();
        r.supplierName = a.getSupplierProduct().getSupplier().getName();
        r.productName = a.getSupplierProduct().getProduct().getName();
        r.type = a.getType(); r.message = a.getMessage();
        r.referenceValue = a.getReferenceValue();
        r.resolved = a.isResolved(); r.createdAt = a.getCreatedAt();
        r.estimatedEventDate = a.getEstimatedEventDate();
        return r;
    }
}
