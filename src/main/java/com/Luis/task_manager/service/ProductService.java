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

/**
 * Servicio de gestión de productos del inventario.
 *
 * <p>Centraliza la lógica de negocio para crear, actualizar y consultar productos,
 * así como para registrar ajustes de stock y verificar alertas de bajo inventario.</p>
 *
 * <p>Cada operación que modifica el stock llama a {@link StockAlertService#checkAndAlert(Product)}
 * para mantener las alertas actualizadas en tiempo real.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;
    private final CategoryService categoryService;
    private final StockAlertService alertService;

    /**
     * Retorna todos los productos activos del inventario.
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findByActiveTrue().stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Busca un producto activo por su ID.
     *
     * @throws ResourceNotFoundException si el producto no existe
     */
    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return ProductResponse.from(getOrThrow(id));
    }

    /**
     * Filtra los productos activos que pertenecen a una categoría específica.
     *
     * @param categoryId ID de la categoría
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> findByCategory(Long categoryId) {
        return productRepository.findByCategoryIdAndActiveTrue(categoryId).stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Busca productos activos cuyo nombre contiene el texto indicado (búsqueda parcial, sin distinción de mayúsculas).
     *
     * @param name texto a buscar en el nombre del producto
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> search(String name) {
        return productRepository.findByNameContainingIgnoreCaseAndActiveTrue(name).stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Retorna los productos cuyo stock está en o por debajo del nivel mínimo configurado.
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> findLowStock() {
        return productRepository.findLowStockProducts().stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Retorna los productos que vencen dentro de los próximos {@code days} días.
     *
     * @param days número de días hacia adelante para verificar vencimiento
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> findExpiring(int days) {
        return productRepository.findExpiringBefore(LocalDate.now().plusDays(days)).stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Crea un nuevo producto en el inventario y verifica si ya requiere una alerta de stock.
     *
     * @param req datos del nuevo producto
     * @return el producto creado
     */
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
        // Verifica alertas en caso de que el stock inicial ya sea bajo
        alertService.checkAndAlert(product);
        return ProductResponse.from(product);
    }

    /**
     * Actualiza los datos de un producto existente.
     * No modifica el stock directamente; para eso usar {@link #adjustStock}.
     *
     * @param id  ID del producto a actualizar
     * @param req nuevos datos del producto
     * @throws ResourceNotFoundException si el producto no existe
     */
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
        // El cambio de minStock puede activar o resolver alertas
        alertService.checkAndAlert(product);
        return ProductResponse.from(product);
    }

    /**
     * Realiza una baja lógica del producto (soft-delete).
     * El producto deja de aparecer en listados y no puede venderse.
     *
     * @param id ID del producto a desactivar
     * @throws ResourceNotFoundException si el producto no existe
     */
    public void deactivate(Long id) {
        Product product = getOrThrow(id);
        product.setActive(false);
        productRepository.save(product);
    }

    /**
     * Ajusta el stock de un producto y registra el movimiento correspondiente.
     *
     * <ul>
     *   <li>{@code ENTRY}: suma la cantidad al stock actual.</li>
     *   <li>{@code EXIT}: resta la cantidad; lanza excepción si no hay suficiente stock.</li>
     *   <li>{@code ADJUSTMENT}: reemplaza el stock actual con la cantidad indicada.</li>
     * </ul>
     *
     * @param id  ID del producto
     * @param req tipo de movimiento, cantidad y motivo
     * @throws IllegalArgumentException  si el tipo es inválido o no hay stock suficiente para EXIT
     * @throws ResourceNotFoundException si el producto no existe
     */
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
            // ADJUSTMENT reemplaza el valor actual con el valor recibido
            case ADJUSTMENT -> req.getQuantity();
        };

        product.setStock(newStock);
        product = productRepository.save(product);

        // Registra el movimiento para el historial de auditoría
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

    /**
     * Retorna el historial de movimientos de stock de un producto, ordenado del más reciente al más antiguo.
     *
     * @param productId ID del producto
     * @throws ResourceNotFoundException si el producto no existe
     */
    @Transactional(readOnly = true)
    public List<StockMovementResponse> getMovements(Long productId) {
        getOrThrow(productId);
        return movementRepository.findByProductIdOrderByDateDesc(productId).stream()
                .map(StockMovementResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Busca un producto activo por su ID o lanza {@link ResourceNotFoundException}.
     * Método utilitario reutilizado por otros servicios (ej. {@link SaleService}).
     */
    public Product getOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
    }

    /**
     * Convierte el tipo de movimiento recibido como String al enum correspondiente.
     *
     * @throws IllegalArgumentException si el valor no corresponde a ningún tipo válido
     */
    private StockMovement.MovementType parseType(String type) {
        if (type == null) return StockMovement.MovementType.ENTRY;
        try {
            return StockMovement.MovementType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de movimiento inválido: " + type + ". Use ENTRY, EXIT o ADJUSTMENT");
        }
    }
}
