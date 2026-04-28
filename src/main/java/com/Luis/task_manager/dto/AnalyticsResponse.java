package com.Luis.task_manager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class AnalyticsResponse {

    private List<TopProduct> topProducts;
    private List<DailyTotal> dailyTotals;
    private List<CategoryBreakdown> categoryBreakdown;
    private List<SellerRanking> sellerRanking;

    @Data @AllArgsConstructor
    public static class TopProduct {
        private String productName;
        private Long quantitySold;
        private BigDecimal revenue;
    }

    @Data @AllArgsConstructor
    public static class DailyTotal {
        private String date;
        private Long salesCount;
        private BigDecimal total;
    }

    @Data @AllArgsConstructor
    public static class CategoryBreakdown {
        private String categoryName;
        private BigDecimal revenue;
    }

    @Data @AllArgsConstructor
    public static class SellerRanking {
        private String username;
        private Long salesCount;
        private BigDecimal total;
    }
}
