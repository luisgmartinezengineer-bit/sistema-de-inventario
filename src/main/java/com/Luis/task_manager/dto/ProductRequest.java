package com.Luis.task_manager.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 300)
    private String description;

    private Long categoryId;

    @NotNull
    @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "Precio inválido (máx. 10 enteros y 2 decimales)")
    private BigDecimal price;

    @NotNull
    @Min(value = 0, message = "El stock no puede ser negativo")
    @Max(value = 9999999, message = "Stock demasiado alto")
    private Integer stock;

    @NotNull
    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    @Max(value = 9999999, message = "Stock mínimo demasiado alto")
    private Integer minStock;

    @Size(max = 30)
    private String unit;

    @Size(max = 50, message = "El código de barras no puede superar 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9\\-]*$", message = "El código de barras solo puede contener letras, dígitos y guiones")
    private String barcode;

    /** IVA permitido: 0, 5 o 19 */
    @DecimalMin(value = "0", message = "IVA mínimo 0%")
    @DecimalMax(value = "19", message = "IVA máximo 19%")
    private BigDecimal taxRate = new BigDecimal("19");

    private LocalDate expirationDate;

    @Size(max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9\\-]*$", message = "El número de lote solo puede contener letras, dígitos y guiones")
    private String lotNumber;
}
