package com.Luis.task_manager.controller;

import com.Luis.task_manager.dto.ProductRequest;
import com.Luis.task_manager.dto.ProductResponse;
import com.Luis.task_manager.dto.StockAdjustRequest;
import com.Luis.task_manager.dto.StockMovementResponse;
import com.Luis.task_manager.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductResponse> findAll(@RequestParam(required = false) String search,
                                          @RequestParam(required = false) Long categoryId) {
        if (search != null && !search.isBlank()) return productService.search(search);
        if (categoryId != null) return productService.findByCategory(categoryId);
        return productService.findAll();
    }

    @GetMapping("/low-stock")
    public List<ProductResponse> lowStock() {
        return productService.findLowStock();
    }

    @GetMapping("/expiring")
    public List<ProductResponse> expiring(@RequestParam(defaultValue = "30") int days) {
        return productService.findExpiring(days);
    }

    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest req) {
        return productService.create(req);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest req) {
        return productService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        productService.deactivate(id);
    }

    @PatchMapping("/{id}/stock")
    public ProductResponse adjustStock(@PathVariable Long id, @Valid @RequestBody StockAdjustRequest req) {
        return productService.adjustStock(id, req);
    }

    @GetMapping("/{id}/movements")
    public List<StockMovementResponse> movements(@PathVariable Long id) {
        return productService.getMovements(id);
    }
}
