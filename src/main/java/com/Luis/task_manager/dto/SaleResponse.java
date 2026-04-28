package com.Luis.task_manager.dto;

import com.Luis.task_manager.entity.Sale;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class SaleResponse {
    private Long id;
    private String invoiceNumber;
    private LocalDateTime date;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal total;
    private String paymentMethod;
    private String notes;
    private Long customerId;
    private String customerName;
    private String customerDocument;
    private String customerAddress;
    private Long cashRegisterId;
    private String cashRegisterName;
    private String sellerName;
    private List<SaleItemResponse> items;

    public static SaleResponse from(Sale sale) {
        SaleResponse r = new SaleResponse();
        r.id = sale.getId();
        r.invoiceNumber = sale.getInvoiceNumber();
        r.date = sale.getDate();
        r.subtotal = sale.getSubtotal();
        r.taxAmount = sale.getTaxAmount();
        r.total = sale.getTotal();
        r.paymentMethod = sale.getPaymentMethod() != null ? sale.getPaymentMethod().name() : "EFECTIVO";
        r.notes = sale.getNotes();
        if (sale.getCustomer() != null) {
            r.customerId = sale.getCustomer().getId();
            r.customerName = sale.getCustomer().getName();
            r.customerDocument = sale.getCustomer().getDocument();
            r.customerAddress = sale.getCustomer().getAddress();
        }
        if (sale.getCashRegister() != null) {
            r.cashRegisterId = sale.getCashRegister().getId();
            r.cashRegisterName = sale.getCashRegister().getName();
        }
        if (sale.getSeller() != null) {
            r.sellerName = sale.getSeller().getFullName();
        }
        r.items = sale.getItems().stream().map(SaleItemResponse::from).collect(Collectors.toList());
        return r;
    }
}
