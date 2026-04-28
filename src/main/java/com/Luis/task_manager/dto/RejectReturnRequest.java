package com.Luis.task_manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RejectReturnRequest {

    @NotBlank
    @Size(max = 300, message = "El motivo de rechazo no puede superar 300 caracteres")
    private String rejectionReason;
}
