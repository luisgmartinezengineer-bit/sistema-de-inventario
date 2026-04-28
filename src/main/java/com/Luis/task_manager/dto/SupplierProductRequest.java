package com.Luis.task_manager.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SupplierProductRequest {

    @NotNull
    private Long supplierId;

    @NotNull
    private Long productId;

    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2, message = "Precio inválido")
    private BigDecimal currentPrice;

    @Min(value = 1, message = "La cantidad mínima de pedido es 1")
    private Integer minOrderQuantity = 1;

    @Size(max = 50)
    private String supplierProductCode;

    private boolean preferred = false;
}
