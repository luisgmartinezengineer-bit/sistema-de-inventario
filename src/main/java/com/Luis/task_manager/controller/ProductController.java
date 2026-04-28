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

/**
 * Controlador REST para la gestión del catálogo de productos.
 *
 * <p>Expone endpoints para consultar, crear, actualizar y desactivar productos,
 * así como para ajustar su stock y consultar el historial de movimientos.</p>
 *
 * <p>Base URL: {@code /api/products}</p>
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Lista todos los productos activos. Soporta filtros opcionales:
     * <ul>
     *   <li>{@code ?search=texto} — búsqueda parcial por nombre</li>
     *   <li>{@code ?categoryId=1} — filtro por categoría</li>
     * </ul>
     */
    @GetMapping
    public List<ProductResponse> findAll(@RequestParam(required = false) String search,
                                          @RequestParam(required = false) Long categoryId) {
        if (search != null && !search.isBlank()) return productService.search(search);
        if (categoryId != null) return productService.findByCategory(categoryId);
        return productService.findAll();
    }

    /**
     * Retorna los productos cuyo stock está en o por debajo del nivel mínimo configurado.
     */
    @GetMapping("/low-stock")
    public List<ProductResponse> lowStock() {
        return productService.findLowStock();
    }

    /**
     * Retorna los productos que vencen dentro de los próximos {@code days} días (por defecto 30).
     */
    @GetMapping("/expiring")
    public List<ProductResponse> expiring(@RequestParam(defaultValue = "30") int days) {
        return productService.findExpiring(days);
    }

    /**
     * Retorna el detalle de un producto por su ID.
     */
    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable Long id) {
        return productService.findById(id);
    }

    /**
     * Crea un nuevo producto en el inventario.
     * Retorna HTTP 201 Created con el producto generado.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest req) {
        return productService.create(req);
    }

    /**
     * Actualiza los datos de un producto existente.
     */
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest req) {
        return productService.update(id, req);
    }

    /**
     * Realiza una baja lógica del producto (soft-delete).
     * El producto deja de ser visible pero sus registros históricos se conservan.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        productService.deactivate(id);
    }

    /**
     * Ajusta el stock de un producto. El campo {@code type} del body puede ser:
     * {@code ENTRY}, {@code EXIT} o {@code ADJUSTMENT}.
     */
    @PatchMapping("/{id}/stock")
    public ProductResponse adjustStock(@PathVariable Long id, @Valid @RequestBody StockAdjustRequest req) {
        return productService.adjustStock(id, req);
    }

    /**
     * Retorna el historial completo de movimientos de stock del producto, ordenado del más reciente.
     */
    @GetMapping("/{id}/movements")
    public List<StockMovementResponse> movements(@PathVariable Long id) {
        return productService.getMovements(id);
    }
}
