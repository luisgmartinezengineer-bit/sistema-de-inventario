package com.Luis.task_manager.dto;

import com.Luis.task_manager.entity.Product;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private String categoryName;
    private Long categoryId;
    private BigDecimal price;
    private BigDecimal taxRate;
    private Integer stock;
    private Integer minStock;
    private String unit;
    private String barcode;
    private boolean active;
    private boolean lowStock;
    private LocalDate expirationDate;
    private String lotNumber;
    private Long daysUntilExpiration;
    private boolean expiringSoon;
    private boolean expired;

    public static ProductResponse from(Product p) {
        ProductResponse r = new ProductResponse();
        r.id = p.getId();
        r.name = p.getName();
        r.description = p.getDescription();
        r.price = p.getPrice();
        r.taxRate = p.getTaxRate() != null ? p.getTaxRate() : new BigDecimal("19");
        r.stock = p.getStock();
        r.minStock = p.getMinStock();
        r.unit = p.getUnit();
        r.barcode = p.getBarcode();
        r.active = p.isActive();
        r.lowStock = p.getStock() <= p.getMinStock();
        r.expirationDate = p.getExpirationDate();
        r.lotNumber = p.getLotNumber();
        if (p.getCategory() != null) {
            r.categoryId = p.getCategory().getId();
            r.categoryName = p.getCategory().getName();
        }
        if (p.getExpirationDate() != null) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), p.getExpirationDate());
            r.daysUntilExpiration = days;
            r.expired = days < 0;
            r.expiringSoon = days >= 0 && days <= 30;
        }
        return r;
    }
}
