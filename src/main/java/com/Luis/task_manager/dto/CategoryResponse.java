package com.Luis.task_manager.dto;

import com.Luis.task_manager.entity.Category;
import lombok.Data;

@Data
public class CategoryResponse {
    private Long id;
    private String name;
    private String description;

    public static CategoryResponse from(Category c) {
        CategoryResponse r = new CategoryResponse();
        r.id = c.getId();
        r.name = c.getName();
        r.description = c.getDescription();
        return r;
    }
}
