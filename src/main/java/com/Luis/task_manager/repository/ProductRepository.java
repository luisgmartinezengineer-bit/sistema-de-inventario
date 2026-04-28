package com.Luis.task_manager.repository;

import com.Luis.task_manager.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrue();

    List<Product> findByCategoryIdAndActiveTrue(Long categoryId);

    @Query("SELECT p FROM Product p WHERE p.stock <= p.minStock AND p.active = true")
    List<Product> findLowStockProducts();

    List<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name);

    @Query("SELECT p FROM Product p WHERE p.expirationDate IS NOT NULL AND p.expirationDate <= :threshold AND p.active = true ORDER BY p.expirationDate ASC")
    List<Product> findExpiringBefore(LocalDate threshold);
}
