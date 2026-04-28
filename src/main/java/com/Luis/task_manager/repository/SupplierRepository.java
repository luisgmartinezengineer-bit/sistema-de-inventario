package com.Luis.task_manager.repository;

import com.Luis.task_manager.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findByActiveTrueOrderByNameAsc();
}
