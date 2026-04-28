package com.Luis.task_manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank
    @Size(max = 80)
    private String name;

    @Size(max = 300)
    private String description;
}
