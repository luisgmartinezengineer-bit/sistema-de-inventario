package com.Luis.task_manager.repository;

import com.Luis.task_manager.entity.PriceQuote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceQuoteRepository extends JpaRepository<PriceQuote, Long> {
    List<PriceQuote> findBySupplierProductIdAndValidTrueOrderByDateDesc(Long supplierProductId);
    List<PriceQuote> findTop2BySupplierProductIdAndValidTrueOrderByDateDesc(Long supplierProductId);
}
