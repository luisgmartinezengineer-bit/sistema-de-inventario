package com.Luis.task_manager.repository;

import com.Luis.task_manager.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByProductIdOrderByDateDesc(Long productId);

    List<StockMovement> findTop50ByOrderByDateDesc();
}
