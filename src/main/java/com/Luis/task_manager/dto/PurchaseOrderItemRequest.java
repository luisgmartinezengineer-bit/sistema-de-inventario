package com.Luis.task_manager.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseOrderItemRequest {

    @NotNull
    private Long productId;

    @NotNull
    @Min(value = 1, message = "La cantidad mínima es 1")
    @Max(value = 9999999, message = "Cantidad demasiado alta")
    private Integer quantity;

    @NotNull
    @DecimalMin(value = "0.01", message = "El costo unitario debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2, message = "Costo inválido (máx. 10 enteros y 2 decimales)")
    private BigDecimal unitCost;
}
