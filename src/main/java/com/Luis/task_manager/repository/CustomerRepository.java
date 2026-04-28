package com.Luis.task_manager.repository;

import com.Luis.task_manager.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByActiveTrueOrderByNameAsc();
    List<Customer> findByNameContainingIgnoreCaseAndActiveTrue(String name);
}
