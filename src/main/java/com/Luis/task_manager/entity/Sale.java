package com.Luis.task_manager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una venta realizada en el sistema.
 *
 * <p>Cada venta contiene una lista de {@link SaleItem} (ítems de venta),
 * un número de factura único generado automáticamente y los totales
 * calculados al momento de la transacción.</p>
 *
 * <p>Al crear una venta, el stock de cada producto se descuenta
 * automáticamente. Si algún ítem no tiene stock suficiente, la venta
 * completa es rechazada.</p>
 */
@Entity
@Table(name = "sales")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Número de factura único, generado según la configuración de la empresa. */
    @Column(unique = true)
    private String invoiceNumber;

    /** Fecha y hora exacta en que se registró la venta. */
    @Column(nullable = false)
    private LocalDateTime date;

    /** Suma de todos los ítems antes de aplicar IVA. */
    @Builder.Default
    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    /** Monto total de IVA calculado sobre los ítems. */
    @Builder.Default
    @Column(precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    /** Total final de la venta: {@code subtotal + taxAmount}. */
    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    /** Ítems que componen la venta. Se eliminan en cascada si se borra la venta. */
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SaleItem> items = new ArrayList<>();

    /** Cliente asociado a la venta. Puede ser nulo para ventas de mostrador. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /** Caja registradora donde se procesó la venta. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cash_register_id")
    private CashRegister cashRegister;

    /** Vendedor que registró la venta. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seller_id")
    private AppUser seller;

    /** Método de pago utilizado. Por defecto: efectivo. */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.EFECTIVO;

    /** Observaciones o notas adicionales sobre la venta. */
    private String notes;

    /** Métodos de pago aceptados por el sistema. */
    public enum PaymentMethod {
        EFECTIVO, TARJETA, TRANSFERENCIA, CREDITO
    }
}
