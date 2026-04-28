package com.Luis.task_manager.dto;

import com.Luis.task_manager.entity.CashRegister;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CashRegisterResponse {
    private Long id;
    private String name;
    private String sellerName;
    private Long sellerId;
    private String status;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private BigDecimal initialAmount;
    private BigDecimal totalSales;
    private BigDecimal totalExpenses;
    private BigDecimal balance;
    private String notes;

    public static CashRegisterResponse from(CashRegister c) {
        CashRegisterResponse r = new CashRegisterResponse();
        r.id = c.getId();
        r.name = c.getName();
        r.status = c.getStatus().name();
        r.openedAt = c.getOpenedAt();
        r.closedAt = c.getClosedAt();
        r.initialAmount = c.getInitialAmount();
        r.totalSales = c.getTotalSales();
        r.totalExpenses = c.getTotalExpenses();
        r.balance = c.getInitialAmount().add(c.getTotalSales()).subtract(c.getTotalExpenses());
        r.notes = c.getNotes();
        if (c.getSeller() != null) {
            r.sellerId = c.getSeller().getId();
            r.sellerName = c.getSeller().getFullName();
        }
        return r;
    }
}
