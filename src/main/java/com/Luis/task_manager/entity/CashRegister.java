package com.Luis.task_manager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_registers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashRegister {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // "Caja 1", "Caja Principal"

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seller_id")
    private AppUser seller;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CashStatus status = CashStatus.CLOSED;

    private LocalDateTime openedAt;
    private LocalDateTime closedAt;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal initialAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalSales = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalExpenses = BigDecimal.ZERO;

    private String notes;

    public enum CashStatus { OPEN, CLOSED }
}
