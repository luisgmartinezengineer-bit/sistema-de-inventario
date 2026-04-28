package com.Luis.task_manager.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseOrderRequest {

    private Long supplierId;

    private LocalDate expectedDeliveryDate;

    @Size(max = 500)
    private String notes;

    @NotEmpty
    @Valid
    private List<PurchaseOrderItemRequest> items;
}
