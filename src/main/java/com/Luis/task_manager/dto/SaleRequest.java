package com.Luis.task_manager.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class SaleRequest {

    private Long customerId;
    private Long cashRegisterId;

    @Pattern(regexp = "^(EFECTIVO|TARJETA|TRANSFERENCIA|CREDITO)$",
             message = "El método de pago debe ser EFECTIVO, TARJETA, TRANSFERENCIA o CREDITO")
    private String paymentMethod;

    @Size(max = 500)
    private String notes;

    @NotEmpty
    @Valid
    private List<SaleItemRequest> items;
}
