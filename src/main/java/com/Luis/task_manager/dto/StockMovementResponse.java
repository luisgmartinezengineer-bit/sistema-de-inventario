package com.Luis.task_manager.dto;

import com.Luis.task_manager.entity.StockMovement;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StockMovementResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String type;
    private Integer quantity;
    private Integer stockBefore;
    private Integer stockAfter;
    private LocalDateTime date;
    private String reason;
    private Long saleId;

    public static StockMovementResponse from(StockMovement m) {
        StockMovementResponse r = new StockMovementResponse();
        r.id = m.getId();
        r.productId = m.getProduct().getId();
        r.productName = m.getProduct().getName();
        r.type = m.getType().name();
        r.quantity = m.getQuantity();
        r.stockBefore = m.getStockBefore();
        r.stockAfter = m.getStockAfter();
        r.date = m.getDate();
        r.reason = m.getReason();
        r.saleId = m.getSaleId();
        return r;
    }
}
