package com.Luis.task_manager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Alerta de bajo inventario generada automáticamente cuando el stock
 * de un producto cae por debajo o iguala su nivel mínimo configurado.
 *
 * <p>El ciclo de vida de una alerta es:</p>
 * <ol>
 *   <li>Se crea con {@code resolved = false} cuando {@code stock <= minStock}.</li>
 *   <li>Se actualiza automáticamente si el stock sigue bajando.</li>
 *   <li>Se resuelve ({@code resolved = true}) cuando el stock se recupera
 *       o un administrador la cierra manualmente.</li>
 * </ol>
 */
@Entity
@Table(name = "stock_alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Producto que generó la alerta. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Stock actual del producto en el momento de crear o actualizar la alerta. */
    @Column(nullable = false)
    private Integer currentStock;

    /** Nivel mínimo de stock configurado en el producto al momento de la alerta. */
    @Column(nullable = false)
    private Integer minStock;

    /** Fecha y hora en que se generó la alerta por primera vez. */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** Indica si la alerta ya fue resuelta (stock recuperado o cierre manual). */
    @Builder.Default
    private boolean resolved = false;

    /** Fecha y hora en que se resolvió la alerta. {@code null} si sigue activa. */
    private LocalDateTime resolvedAt;
}
