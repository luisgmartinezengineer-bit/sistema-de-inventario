package com.Luis.task_manager.repository;

import com.Luis.task_manager.entity.SaleReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleReturnRepository extends JpaRepository<SaleReturn, Long> {
    List<SaleReturn> findBySaleIdOrderByRequestDateDesc(Long saleId);
    List<SaleReturn> findAllByOrderByRequestDateDesc();
    List<SaleReturn> findByStatusOrderByRequestDateAsc(SaleReturn.ReturnStatus status);
    List<SaleReturn> findByRequestedByUsernameOrderByRequestDateDesc(String username);
}
