package com.Luis.task_manager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Registra cada movimiento de stock de un producto: entradas, salidas y ajustes manuales.
 *
 * <p>Esta tabla actúa como bitácora de auditoría del inventario, permitiendo
 * rastrear el histórico completo de cambios en el stock de cualquier producto.</p>
 */
@Entity
@Table(name = "stock_movements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Producto al que corresponde este movimiento. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Tipo de movimiento: entrada, salida o ajuste. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementType type;

    /** Cantidad de unidades involucradas en el movimiento. */
    @Column(nullable = false)
    private Integer quantity;

    /** Stock del producto antes de aplicar el movimiento. */
    @Column(nullable = false)
    private Integer stockBefore;

    /** Stock del producto después de aplicar el movimiento. */
    @Column(nullable = false)
    private Integer stockAfter;

    /** Fecha y hora en que ocurrió el movimiento. */
    @Column(nullable = false)
    private LocalDateTime date;

    /** Motivo del movimiento (ej. "Compra proveedor X", "Merma", "Venta FV00000001"). */
    private String reason;

    /**
     * ID de la venta que originó este movimiento.
     * Es {@code null} cuando el movimiento fue registrado manualmente.
     */
    private Long saleId;

    /** Tipos de movimiento de inventario soportados por el sistema. */
    public enum MovementType {
        /** Ingreso de mercancía al inventario (compra, devolución de cliente, etc.). */
        ENTRY,
        /** Salida de mercancía del inventario (venta, merma, etc.). */
        EXIT,
        /** Corrección directa del stock (inventario físico, ajuste administrativo). */
        ADJUSTMENT
    }
}
