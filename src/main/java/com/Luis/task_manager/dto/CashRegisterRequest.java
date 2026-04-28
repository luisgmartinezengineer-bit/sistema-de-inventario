package com.Luis.task_manager.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CashRegisterRequest {

    @NotBlank
    @Size(max = 80)
    private String name;

    private Long sellerId;

    @NotNull
    @DecimalMin(value = "0.0", message = "El monto inicial no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "Monto inválido")
    private BigDecimal initialAmount;

    @Size(max = 300)
    private String notes;
}
