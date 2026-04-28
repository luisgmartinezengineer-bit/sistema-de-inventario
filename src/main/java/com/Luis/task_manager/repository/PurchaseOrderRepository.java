package com.Luis.task_manager.repository;

import com.Luis.task_manager.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    List<PurchaseOrder> findAllByOrderByCreatedAtDesc();

    List<PurchaseOrder> findByStatusOrderByCreatedAtDesc(PurchaseOrder.OrderStatus status);

    List<PurchaseOrder> findBySupplierIdOrderByCreatedAtDesc(Long supplierId);
}
