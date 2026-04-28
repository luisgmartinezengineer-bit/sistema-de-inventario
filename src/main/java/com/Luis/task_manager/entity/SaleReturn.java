package com.Luis.task_manager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sale_returns")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @Column(nullable = false)
    private LocalDateTime requestDate;

    @Column(nullable = false)
    private String reason;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal refundTotal = BigDecimal.ZERO;

    private String notes;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private ReturnStatus status = ReturnStatus.PENDIENTE;

    /** Usuario que solicita la devolución (cualquier rol) */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requested_by_id")
    private AppUser requestedBy;

    /** Supervisor o Admin que aprueba/rechaza */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "processed_by_id")
    private AppUser processedBy;

    private LocalDateTime processedDate;

    private String rejectionReason;

    @OneToMany(mappedBy = "saleReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SaleReturnItem> items = new ArrayList<>();

    public enum ReturnStatus { PENDIENTE, APROBADA, RECHAZADA }
}
