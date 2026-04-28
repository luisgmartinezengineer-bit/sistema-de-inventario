package com.Luis.task_manager.repository;

import com.Luis.task_manager.entity.CompanyConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyConfigRepository extends JpaRepository<CompanyConfig, Long> {
}
