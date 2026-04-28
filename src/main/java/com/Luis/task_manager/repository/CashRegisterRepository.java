package com.Luis.task_manager.repository;

import com.Luis.task_manager.entity.CashRegister;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CashRegisterRepository extends JpaRepository<CashRegister, Long> {
    List<CashRegister> findByStatusOrderByOpenedAtDesc(CashRegister.CashStatus status);
    List<CashRegister> findAllByOrderByOpenedAtDesc();
    Optional<CashRegister> findBySellerIdAndStatus(Long sellerId, CashRegister.CashStatus status);
    List<CashRegister> findAllBySellerIdAndStatus(Long sellerId, CashRegister.CashStatus status);
}
