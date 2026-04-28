package com.Luis.task_manager.service;

import com.Luis.task_manager.dto.ProductRequest;
import com.Luis.task_manager.dto.ProductResponse;
import com.Luis.task_manager.dto.StockAdjustRequest;
import com.Luis.task_manager.dto.StockMovementResponse;
import com.Luis.task_manager.entity.Category;
import com.Luis.task_manager.entity.Product;
import com.Luis.task_manager.entity.StockMovement;
import com.Luis.task_manager.exception.ResourceNotFoundException;
import com.Luis.task_manager.repository.ProductRepository;
import com.Luis.task_manager.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;
    private final CategoryService categoryService;
    private final StockAlertService alertService;

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findByActiveTrue().stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return ProductResponse.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findByCategory(Long categoryId) {
        return productRepository.findByCategoryIdAndActiveTrue(categoryId).stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> search(String name) {
        return productRepository.findByNameContainingIgnoreCaseAndActiveTrue(name).stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findLowStock() {
        return productRepository.findLowStockProducts().stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findExpiring(int days) {
        return productRepository.findExpiringBefore(LocalDate.now().plusDays(days)).stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    public ProductResponse create(ProductRequest req) {
        Category category = req.getCategoryId() != null
                ? categoryService.getOrThrow(req.getCategoryId())
                : null;

        Product product = Product.builder()
                .name(req.getName())
                .description(req.getDescription())
                .category(category)
                .price(req.getPrice())
                .taxRate(req.getTaxRate())
                .barcode(req.getBarcode())
                .stock(req.getStock())
                .minStock(req.getMinStock())
                .unit(req.getUnit())
                .expirationDate(req.getExpirationDate())
                .lotNumber(req.getLotNumber())
                .active(true)
                .build();

        product = productRepository.save(product);
        alertService.checkAndAlert(product);
        return ProductResponse.from(product);
    }

    public ProductResponse update(Long id, ProductRequest req) {
        Product product = getOrThrow(id);
        Category category = req.getCategoryId() != null
                ? categoryService.getOrThrow(req.getCategoryId())
                : null;

        product.setName(req.getName());
        product.setDescription(req.getDescription());
        product.setCategory(category);
        product.setPrice(req.getPrice());
        product.setTaxRate(req.getTaxRate());
        product.setBarcode(req.getBarcode());
        product.setMinStock(req.getMinStock());
        product.setUnit(req.getUnit());
        product.setExpirationDate(req.getExpirationDate());
        product.setLotNumber(req.getLotNumber());

        product = productRepository.save(product);
        alertService.checkAndAlert(product);
        return ProductResponse.from(product);
    }

    public void deactivate(Long id) {
        Product product = getOrThrow(id);
        product.setActive(false);
        productRepository.save(product);
    }

    public ProductResponse adjustStock(Long id, StockAdjustRequest req) {
        Product product = getOrThrow(id);
        int before = product.getStock();

        StockMovement.MovementType type = parseType(req.getType());
        int newStock = switch (type) {
            case ENTRY -> before + req.getQuantity();
            case EXIT -> {
                if (before < req.getQuantity()) {
                    throw new IllegalArgumentException(
                            "Stock insuficiente: disponible=%d, solicitado=%d".formatted(before, req.getQuantity()));
                }
                yield before - req.getQuantity();
            }
            case ADJUSTMENT -> req.getQuantity();
        };

        product.setStock(newStock);
        product = productRepository.save(product);

        movementRepository.save(StockMovement.builder()
                .product(product)
                .type(type)
                .quantity(req.getQuantity())
                .stockBefore(before)
                .stockAfter(newStock)
                .date(LocalDateTime.now())
                .reason(req.getReason())
                .build());

        alertService.checkAndAlert(product);
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> getMovements(Long productId) {
        getOrThrow(productId);
        return movementRepository.findByProductIdOrderByDateDesc(productId).stream()
                .map(StockMovementResponse::from)
                .collect(Collectors.toList());
    }

    public Product getOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
    }

    private StockMovement.MovementType parseType(String type) {
        if (type == null) return StockMovement.MovementType.ENTRY;
        try {
            return StockMovement.MovementType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de movimiento inválido: " + type + ". Use ENTRY, EXIT o ADJUSTMENT");
        }
    }
}
