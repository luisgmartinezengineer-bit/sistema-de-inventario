package com.Luis.task_manager.repository;

import com.Luis.task_manager.entity.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {

    List<StockAlert> findByResolvedFalseOrderByCreatedAtDesc();

    List<StockAlert> findAllByOrderByCreatedAtDesc();

    Optional<StockAlert> findByProductIdAndResolvedFalse(Long productId);

    boolean existsByProductIdAndResolvedFalse(Long productId);
}
