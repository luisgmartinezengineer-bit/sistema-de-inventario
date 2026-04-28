package com.Luis.task_manager.service;

import com.Luis.task_manager.dto.AnalyticsResponse;
import com.Luis.task_manager.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final SaleRepository saleRepository;

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(int days) {
        LocalDateTime from = LocalDate.now().minusDays(days - 1).atStartOfDay();
        LocalDateTime to = LocalDate.now().plusDays(1).atStartOfDay();

        List<AnalyticsResponse.TopProduct> topProducts = saleRepository.findTopProductsBetween(from, to)
                .stream().limit(10).map(row -> new AnalyticsResponse.TopProduct(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        (BigDecimal) row[2]))
                .collect(Collectors.toList());

        List<AnalyticsResponse.DailyTotal> dailyTotals = saleRepository.findDailyTotalsBetween(from, to)
                .stream().map(row -> new AnalyticsResponse.DailyTotal(
                        row[0].toString(),
                        ((Number) row[1]).longValue(),
                        (BigDecimal) row[2]))
                .collect(Collectors.toList());

        List<AnalyticsResponse.CategoryBreakdown> categoryBreakdown = saleRepository.findSalesByCategoryBetween(from, to)
                .stream().map(row -> new AnalyticsResponse.CategoryBreakdown(
                        (String) row[0],
                        (BigDecimal) row[1]))
                .collect(Collectors.toList());

        List<AnalyticsResponse.SellerRanking> sellerRanking = saleRepository.findSellerRankingBetween(from, to)
                .stream().map(row -> new AnalyticsResponse.SellerRanking(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        (BigDecimal) row[2]))
                .collect(Collectors.toList());

        return new AnalyticsResponse(topProducts, dailyTotals, categoryBreakdown, sellerRanking);
    }
}
