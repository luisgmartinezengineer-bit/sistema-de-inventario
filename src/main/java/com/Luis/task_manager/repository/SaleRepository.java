package com.Luis.task_manager.repository;

import com.Luis.task_manager.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findByDateBetweenOrderByDateDesc(LocalDateTime from, LocalDateTime to);

    @Query("SELECT COALESCE(SUM(s.total), 0) FROM Sale s WHERE s.date BETWEEN :from AND :to")
    BigDecimal sumTotalBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<Sale> findTop10ByOrderByDateDesc();

    List<Sale> findByCashRegisterIdOrderByDateDesc(Long cashRegisterId);

    List<Sale> findByCashRegisterIdAndDateBetweenOrderByDateDesc(Long cashRegisterId, LocalDateTime from, LocalDateTime to);

    List<Sale> findByInvoiceNumberContainingIgnoreCaseOrderByDateDesc(String invoiceNumber);

    @Query("SELECT si.product.name, SUM(si.quantity) as qty, SUM(si.subtotal) as revenue " +
           "FROM SaleItem si WHERE si.sale.date BETWEEN :from AND :to " +
           "GROUP BY si.product.id, si.product.name ORDER BY qty DESC")
    List<Object[]> findTopProductsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT FUNCTION('DATE', s.date) as day, COUNT(s), SUM(s.total) " +
           "FROM Sale s WHERE s.date BETWEEN :from AND :to GROUP BY FUNCTION('DATE', s.date) ORDER BY day ASC")
    List<Object[]> findDailyTotalsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT si.product.category.name, SUM(si.subtotal) " +
           "FROM SaleItem si WHERE si.sale.date BETWEEN :from AND :to AND si.product.category IS NOT NULL " +
           "GROUP BY si.product.category.id, si.product.category.name ORDER BY SUM(si.subtotal) DESC")
    List<Object[]> findSalesByCategoryBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT s.seller.username, COUNT(s), SUM(s.total) " +
           "FROM Sale s WHERE s.date BETWEEN :from AND :to AND s.seller IS NOT NULL " +
           "GROUP BY s.seller.id, s.seller.username ORDER BY SUM(s.total) DESC")
    List<Object[]> findSellerRankingBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
