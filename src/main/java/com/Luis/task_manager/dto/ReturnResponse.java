package com.Luis.task_manager.dto;

import com.Luis.task_manager.entity.SaleReturn;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ReturnResponse {
    private Long id;
    private Long saleId;
    private String invoiceNumber;
    private LocalDateTime requestDate;
    private LocalDateTime processedDate;
    private String reason;
    private String notes;
    private BigDecimal refundTotal;
    private String status;
    private String requestedBy;
    private String processedBy;
    private String rejectionReason;
    private List<ReturnItemResponse> items;

    public static ReturnResponse from(SaleReturn r) {
        ReturnResponse res = new ReturnResponse();
        res.id = r.getId();
        res.saleId = r.getSale().getId();
        res.invoiceNumber = r.getSale().getInvoiceNumber();
        res.requestDate = r.getRequestDate();
        res.processedDate = r.getProcessedDate();
        res.reason = r.getReason();
        res.notes = r.getNotes();
        res.refundTotal = r.getRefundTotal();
        res.status = r.getStatus().name();
        res.requestedBy = r.getRequestedBy() != null ? r.getRequestedBy().getFullName() : null;
        res.processedBy = r.getProcessedBy() != null ? r.getProcessedBy().getFullName() : null;
        res.rejectionReason = r.getRejectionReason();
        res.items = r.getItems().stream().map(ReturnItemResponse::from).collect(Collectors.toList());
        return res;
    }
}
