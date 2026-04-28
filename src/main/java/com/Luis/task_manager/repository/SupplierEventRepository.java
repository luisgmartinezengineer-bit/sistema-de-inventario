package com.Luis.task_manager.repository;

import com.Luis.task_manager.entity.SupplierEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SupplierEventRepository extends JpaRepository<SupplierEvent, Long> {

    List<SupplierEvent> findAllByOrderByStartDateDesc();

    @Query("SELECT e FROM SupplierEvent e WHERE e.startDate <= :future AND (e.endDate IS NULL OR e.endDate >= :today)")
    List<SupplierEvent> findActiveOrUpcoming(@Param("today") LocalDate today, @Param("future") LocalDate future);
}
