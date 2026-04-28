package com.Luis.task_manager.dto;

import com.Luis.task_manager.entity.PurchaseOrderItem;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseOrderItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitCost;
    private BigDecimal subtotal;

    public static PurchaseOrderItemResponse from(PurchaseOrderItem i) {
        PurchaseOrderItemResponse r = new PurchaseOrderItemResponse();
        r.id = i.getId();
        r.productId = i.getProduct().getId();
        r.productName = i.getProduct().getName();
        r.quantity = i.getQuantity();
        r.unitCost = i.getUnitCost();
        r.subtotal = i.getSubtotal();
        return r;
    }
}
