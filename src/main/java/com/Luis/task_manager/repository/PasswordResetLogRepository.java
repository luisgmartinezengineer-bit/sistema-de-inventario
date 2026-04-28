package com.Luis.task_manager.repository;

import com.Luis.task_manager.entity.PasswordResetLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PasswordResetLogRepository extends JpaRepository<PasswordResetLog, Long> {
    List<PasswordResetLog> findAllByOrderByCreatedAtDesc();
}
