package com.Luis.task_manager.dto;

import com.Luis.task_manager.entity.PriceQuote;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PriceQuoteResponse {
    private Long id;
    private Long supplierProductId;
    private String supplierName;
    private String productName;
    private BigDecimal price;
    private LocalDateTime date;
    private String origin;
    private String note;
    private BigDecimal variationPercent;
    private boolean valid;

    public static PriceQuoteResponse from(PriceQuote q) {
        PriceQuoteResponse r = new PriceQuoteResponse();
        r.id = q.getId();
        r.supplierProductId = q.getSupplierProduct().getId();
        r.supplierName = q.getSupplierProduct().getSupplier().getName();
        r.productName = q.getSupplierProduct().getProduct().getName();
        r.price = q.getPrice();
        r.date = q.getDate();
        r.origin = q.getOrigin();
        r.note = q.getNote();
        r.variationPercent = q.getVariationPercent();
        r.valid = q.isValid();
        return r;
    }
}
