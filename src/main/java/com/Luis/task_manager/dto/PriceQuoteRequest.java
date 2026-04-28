package com.Luis.task_manager.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PriceQuoteRequest {

    @NotNull
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2, message = "Precio inválido (máx. 10 enteros y 2 decimales)")
    private BigDecimal price;

    @Size(max = 300)
    private String note;
}
