package com.Luis.task_manager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer currentStock;

    @Column(nullable = false)
    private Integer minStock;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    private boolean resolved = false;

    private LocalDateTime resolvedAt;
}
