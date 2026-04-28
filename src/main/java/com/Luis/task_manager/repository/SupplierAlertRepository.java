package com.Luis.task_manager.repository;

import com.Luis.task_manager.entity.SupplierAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SupplierAlertRepository extends JpaRepository<SupplierAlert, Long> {
    List<SupplierAlert> findByResolvedFalseOrderByCreatedAtDesc();
    List<SupplierAlert> findAllByOrderByCreatedAtDesc();
    boolean existsBySupplierProductIdAndTypeAndResolvedFalseAndCreatedAtAfter(
            Long supplierProductId, String type, LocalDateTime after);
}
