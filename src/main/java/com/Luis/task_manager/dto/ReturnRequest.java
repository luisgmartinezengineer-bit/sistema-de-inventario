package com.Luis.task_manager.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class ReturnRequest {

    @NotNull
    private Long saleId;

    @NotBlank
    @Size(max = 300)
    private String reason;

    @Size(max = 500)
    private String notes;

    @NotEmpty
    @Valid
    private List<ReturnItemRequest> items;
}
