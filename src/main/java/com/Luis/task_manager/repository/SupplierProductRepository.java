package com.Luis.task_manager.repository;

import com.Luis.task_manager.entity.SupplierProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierProductRepository extends JpaRepository<SupplierProduct, Long> {
    List<SupplierProduct> findBySupplierIdAndActiveTrue(Long supplierId);
    List<SupplierProduct> findByProductIdAndActiveTrue(Long productId);
    Optional<SupplierProduct> findBySupplierIdAndProductId(Long supplierId, Long productId);
}
