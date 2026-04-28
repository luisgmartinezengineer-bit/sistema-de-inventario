package com.Luis.task_manager.dto;

import com.Luis.task_manager.entity.PurchaseOrder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class PurchaseOrderResponse {
    private Long id;
    private String orderNumber;
    private Long supplierId;
    private String supplierName;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private LocalDate expectedDeliveryDate;
    private String status;
    private BigDecimal total;
    private String notes;
    private List<PurchaseOrderItemResponse> items;

    public static PurchaseOrderResponse from(PurchaseOrder o) {
        PurchaseOrderResponse r = new PurchaseOrderResponse();
        r.id = o.getId();
        r.orderNumber = o.getOrderNumber();
        r.supplierId = o.getSupplier() != null ? o.getSupplier().getId() : null;
        r.supplierName = o.getSupplier() != null ? o.getSupplier().getName() : null;
        r.createdByUsername = o.getCreatedBy() != null ? o.getCreatedBy().getUsername() : null;
        r.createdAt = o.getCreatedAt();
        r.expectedDeliveryDate = o.getExpectedDeliveryDate();
        r.status = o.getStatus().name();
        r.total = o.getTotal();
        r.notes = o.getNotes();
        r.items = o.getItems().stream().map(PurchaseOrderItemResponse::from).collect(Collectors.toList());
        return r;
    }
}
