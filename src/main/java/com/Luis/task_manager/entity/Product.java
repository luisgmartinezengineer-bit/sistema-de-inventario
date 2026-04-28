package com.Luis.task_manager.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidad principal del inventario. Representa un producto con su precio,
 * stock actual y nivel mínimo de stock para generar alertas.
 *
 * <p>El borrado de productos es lógico: en lugar de eliminarse de la base de datos,
 * se marca {@code active = false} y se excluye de todos los listados.</p>
 */
@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre del producto; no puede estar vacío. */
    @NotBlank
    @Column(nullable = false)
    private String name;

    /** Descripción detallada del producto (opcional). */
    private String description;

    /** Categoría a la que pertenece el producto. Puede ser nula. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    /** Precio de venta unitario; debe ser mayor o igual a 0. */
    @NotNull
    @DecimalMin("0.0")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /** Cantidad de unidades disponibles en bodega. */
    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer stock;

    /**
     * Stock mínimo permitido. Cuando {@code stock <= minStock} se genera
     * automáticamente una alerta de bajo inventario.
     */
    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer minStock;

    /** Unidad de medida (ej. "unidad", "kg", "litro"). */
    private String unit;

    /** Código de barras único del producto (opcional). */
    @Column(unique = true)
    private String barcode;

    /**
     * Porcentaje de IVA aplicable al producto.
     * Valores válidos según la DIAN: 0, 5 o 19.
     * Por defecto se asume tarifa general del 19%.
     */
    @Builder.Default
    @Column(precision = 5, scale = 2)
    private BigDecimal taxRate = new BigDecimal("19");

    /** Fecha de vencimiento del producto (opcional, para perecederos). */
    private LocalDate expirationDate;

    /** Número de lote para trazabilidad (opcional). */
    private String lotNumber;

    /**
     * Indica si el producto está activo en el sistema.
     * Los productos inactivos no aparecen en búsquedas ni ventas.
     */
    @Builder.Default
    private boolean active = true;
}
