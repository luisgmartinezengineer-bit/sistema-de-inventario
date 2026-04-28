package com.Luis.task_manager.dto;

import com.Luis.task_manager.entity.SupplierEvent;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SupplierEventResponse {
    private Long id;
    private Long supplierProductId;
    private String supplierName;
    private String productName;
    private String type;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String expectedImpact;
    private String intensity;
    private boolean active; // si está dentro del rango de fechas actual

    public static SupplierEventResponse from(SupplierEvent e) {
        SupplierEventResponse r = new SupplierEventResponse();
        r.id = e.getId();
        if (e.getSupplierProduct() != null) {
            r.supplierProductId = e.getSupplierProduct().getId();
            r.supplierName = e.getSupplierProduct().getSupplier().getName();
            r.productName = e.getSupplierProduct().getProduct().getName();
        }
        r.type = e.getType(); r.description = e.getDescription();
        r.startDate = e.getStartDate(); r.endDate = e.getEndDate();
        r.expectedImpact = e.getExpectedImpact(); r.intensity = e.getIntensity();
        LocalDate today = LocalDate.now();
        r.active = !today.isBefore(e.getStartDate()) && (e.getEndDate() == null || !today.isAfter(e.getEndDate()));
        return r;
    }
}
