package com.Luis.task_manager.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SupplierEventRequest {

    private Long supplierProductId;

    @NotBlank
    @Pattern(regexp = "^(TEMPORADA_ALTA|ESCASEZ|PARO|FERIA|COSECHA|OTRO)$",
             message = "Tipo de evento inválido")
    private String type;

    @NotBlank
    @Size(max = 300)
    private String description;

    @NotNull
    private LocalDate startDate;

    private LocalDate endDate;

    @Pattern(regexp = "^(SUBE_PRECIO|BAJA_PRECIO|AUMENTA_DEMANDA)?$",
             message = "Impacto esperado inválido")
    private String expectedImpact;

    @Pattern(regexp = "^(BAJA|MEDIA|ALTA)$", message = "La intensidad debe ser BAJA, MEDIA o ALTA")
    private String intensity = "MEDIA";
}
