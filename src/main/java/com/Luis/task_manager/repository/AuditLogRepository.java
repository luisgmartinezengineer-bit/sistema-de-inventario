package com.Luis.task_manager.repository;

import com.Luis.task_manager.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);

    Page<AuditLog> findByUsernameContainingIgnoreCaseOrderByTimestampDesc(String username, Pageable pageable);

    Page<AuditLog> findByEntityTypeOrderByTimestampDesc(String entityType, Pageable pageable);

    Page<AuditLog> findByUsernameContainingIgnoreCaseAndEntityTypeOrderByTimestampDesc(
            String username, String entityType, Pageable pageable);

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:username IS NULL OR LOWER(a.username) LIKE LOWER(CONCAT('%',:username,'%')))
          AND (:entityType IS NULL OR a.entityType = :entityType)
          AND (:action IS NULL OR a.action = :action)
          AND (:from IS NULL OR a.timestamp >= :from)
          AND (:to IS NULL OR a.timestamp <= :to)
        ORDER BY a.timestamp DESC
    """)
    Page<AuditLog> findWithFilters(
            @Param("username") String username,
            @Param("entityType") String entityType,
            @Param("action") String action,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.timestamp >= :from")
    long countSince(@Param("from") LocalDateTime from);

    @Query("SELECT a.action, COUNT(a) FROM AuditLog a GROUP BY a.action ORDER BY COUNT(a) DESC")
    List<Object[]> countByAction();

    @Query("SELECT a.entityType, COUNT(a) FROM AuditLog a GROUP BY a.entityType ORDER BY COUNT(a) DESC")
    List<Object[]> countByEntityType();

    @Query("SELECT a.username, COUNT(a) FROM AuditLog a WHERE a.timestamp >= :from GROUP BY a.username ORDER BY COUNT(a) DESC")
    List<Object[]> topUsersSince(@Param("from") LocalDateTime from);
}
