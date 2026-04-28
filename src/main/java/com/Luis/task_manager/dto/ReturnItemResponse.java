package com.Luis.task_manager.dto;

import com.Luis.task_manager.entity.SaleReturnItem;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReturnItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    public static ReturnItemResponse from(SaleReturnItem item) {
        ReturnItemResponse r = new ReturnItemResponse();
        r.id = item.getId();
        r.productId = item.getProduct().getId();
        r.productName = item.getProduct().getName();
        r.quantity = item.getQuantity();
        r.unitPrice = item.getUnitPrice();
        r.subtotal = item.getSubtotal();
        return r;
    }
}
