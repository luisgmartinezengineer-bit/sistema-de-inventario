package com.Luis.task_manager.dto;

import com.Luis.task_manager.entity.StockAlert;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StockAlertResponse {
    private Long id;
    private Long productId;
    private String productName;
    private Integer currentStock;
    private Integer minStock;
    private LocalDateTime createdAt;
    private boolean resolved;
    private LocalDateTime resolvedAt;

    public static StockAlertResponse from(StockAlert a) {
        StockAlertResponse r = new StockAlertResponse();
        r.id = a.getId();
        r.productId = a.getProduct().getId();
        r.productName = a.getProduct().getName();
        r.currentStock = a.getCurrentStock();
        r.minStock = a.getMinStock();
        r.createdAt = a.getCreatedAt();
        r.resolved = a.isResolved();
        r.resolvedAt = a.getResolvedAt();
        return r;
    }
}
