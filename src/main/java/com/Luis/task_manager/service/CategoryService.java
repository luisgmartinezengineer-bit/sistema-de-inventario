package com.Luis.task_manager.service;

import com.Luis.task_manager.dto.CategoryRequest;
import com.Luis.task_manager.dto.CategoryResponse;
import com.Luis.task_manager.entity.Category;
import com.Luis.task_manager.exception.ResourceNotFoundException;
import com.Luis.task_manager.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

    public CategoryResponse findById(Long id) {
        return CategoryResponse.from(getOrThrow(id));
    }

    public CategoryResponse create(CategoryRequest req) {
        if (categoryRepository.existsByNameIgnoreCase(req.getName())) {
            throw new IllegalArgumentException("Ya existe una categoría con el nombre: " + req.getName());
        }
        Category category = Category.builder()
                .name(req.getName())
                .description(req.getDescription())
                .build();
        return CategoryResponse.from(categoryRepository.save(category));
    }

    public CategoryResponse update(Long id, CategoryRequest req) {
        Category category = getOrThrow(id);
        category.setName(req.getName());
        category.setDescription(req.getDescription());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    public void delete(Long id) {
        getOrThrow(id);
        categoryRepository.deleteById(id);
    }

    public Category getOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + id));
    }
}
