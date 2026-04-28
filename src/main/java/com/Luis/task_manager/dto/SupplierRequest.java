package com.Luis.task_manager.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SupplierRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    /** NIT colombiano: dígitos (6-15), opcionalmente con dígito de verificación */
    @Pattern(regexp = "^[0-9]{6,15}(-[0-9])?$", message = "NIT inválido (ej: 900123456-7)")
    @Size(max = 20)
    private String nit;

    @Size(max = 100)
    private String contactName;

    @Email(message = "El correo electrónico no tiene un formato válido")
    @Size(max = 100)
    private String email;

    @Pattern(regexp = "^[+]?[0-9][0-9\\-\\s()]{6,18}$", message = "Teléfono inválido (ej: 3001234567 o +57 300 123 4567)")
    private String phone;

    @Size(max = 80)
    private String city;

    @Size(max = 200)
    private String address;

    @Min(value = 0, message = "El plazo de pago no puede ser negativo")
    @Max(value = 365, message = "El plazo de pago no puede superar 365 días")
    private Integer paymentTermsDays = 0;

    @Min(value = 1, message = "El lead time mínimo es 1 día")
    @Max(value = 365, message = "El lead time no puede superar 365 días")
    private Integer leadTimeDays = 1;

    @DecimalMin(value = "1.0", message = "El rating mínimo es 1")
    @DecimalMax(value = "5.0", message = "El rating máximo es 5")
    private BigDecimal rating;

    @Size(max = 500)
    private String notes;
}
