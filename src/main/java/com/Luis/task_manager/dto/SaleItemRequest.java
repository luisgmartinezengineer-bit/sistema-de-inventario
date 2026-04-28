package com.Luis.task_manager.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaleItemRequest {

    @NotNull
    private Long productId;

    @NotNull
    @Min(value = 1, message = "La cantidad mínima es 1")
    @Max(value = 9999999, message = "Cantidad demasiado alta")
    private Integer quantity;

    @DecimalMin(value = "0.0", message = "El descuento no puede ser negativo")
    @DecimalMax(value = "100.0", message = "El descuento no puede superar el 100%")
    @Digits(integer = 3, fraction = 2, message = "Descuento inválido")
    private BigDecimal discountPercent = BigDecimal.ZERO;
}
