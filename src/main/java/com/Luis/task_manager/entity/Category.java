package com.Luis.task_manager.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Representa una categoría para agrupar productos del inventario.
 * Cada producto puede pertenecer a una única categoría.
 */
@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre único de la categoría (ej. "Bebidas", "Lácteos"). */
    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    /** Descripción opcional para dar más contexto a la categoría. */
    private String description;
}
