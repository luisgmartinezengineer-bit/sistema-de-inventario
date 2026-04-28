package com.Luis.task_manager.dto;

import com.Luis.task_manager.entity.SupplierProduct;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SupplierProductResponse {
    private Long id;
    private Long supplierId;
    private String supplierName;
    private Long productId;
    private String productName;
    private String productBarcode;
    private BigDecimal currentPrice;
    private BigDecimal minHistoricalPrice;
    private BigDecimal maxHistoricalPrice;
    private Integer minOrderQuantity;
    private String supplierProductCode;
    private boolean preferred;
    private LocalDateTime lastQuoteDate;
    private boolean active;

    public static SupplierProductResponse from(SupplierProduct sp) {
        SupplierProductResponse r = new SupplierProductResponse();
        r.id = sp.getId();
        r.supplierId = sp.getSupplier().getId();
        r.supplierName = sp.getSupplier().getName();
        r.productId = sp.getProduct().getId();
        r.productName = sp.getProduct().getName();
        r.productBarcode = sp.getProduct().getBarcode();
        r.currentPrice = sp.getCurrentPrice();
        r.minHistoricalPrice = sp.getMinHistoricalPrice();
        r.maxHistoricalPrice = sp.getMaxHistoricalPrice();
        r.minOrderQuantity = sp.getMinOrderQuantity();
        r.supplierProductCode = sp.getSupplierProductCode();
        r.preferred = sp.isPreferred();
        r.lastQuoteDate = sp.getLastQuoteDate();
        r.active = sp.isActive();
        return r;
    }
}
