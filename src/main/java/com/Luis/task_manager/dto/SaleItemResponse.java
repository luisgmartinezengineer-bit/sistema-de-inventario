package com.Luis.task_manager.dto;

import com.Luis.task_manager.entity.SaleItem;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaleItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountPercent;
    private BigDecimal discountAmount;
    private BigDecimal subtotal;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;

    public static SaleItemResponse from(SaleItem item) {
        SaleItemResponse r = new SaleItemResponse();
        r.id = item.getId();
        r.productId = item.getProduct().getId();
        r.productName = item.getProduct().getName();
        r.quantity = item.getQuantity();
        r.unitPrice = item.getUnitPrice();
        r.discountPercent = item.getDiscountPercent();
        r.discountAmount = item.getDiscountAmount();
        r.subtotal = item.getSubtotal();
        r.taxRate = item.getTaxRate();
        r.taxAmount = item.getTaxAmount();
        return r;
    }
}
