package com.Luis.task_manager.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class StockAdjustRequest {

    @NotNull
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Max(value = 9999999, message = "Cantidad demasiado alta")
    private Integer quantity;

    @Size(max = 300)
    private String reason;

    /** Valores permitidos: ENTRY, EXIT, ADJUSTMENT */
    @NotBlank
    @Pattern(regexp = "^(ENTRY|EXIT|ADJUSTMENT)$", message = "El tipo debe ser ENTRY, EXIT o ADJUSTMENT")
    private String type;
}
