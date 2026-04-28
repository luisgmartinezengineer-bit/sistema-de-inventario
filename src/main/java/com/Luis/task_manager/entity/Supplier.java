package com.Luis.task_manager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "suppliers")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Supplier {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String nit;
    private String contactName;
    private String email;
    private String phone;
    private String city;
    private String address;

    @Builder.Default
    private Integer paymentTermsDays = 0;   // días plazo de pago

    @Builder.Default
    private Integer leadTimeDays = 1;        // días de entrega promedio

    @Builder.Default
    private BigDecimal rating = BigDecimal.valueOf(3); // 1-5

    private String notes;

    @Builder.Default
    private boolean active = true;
}
