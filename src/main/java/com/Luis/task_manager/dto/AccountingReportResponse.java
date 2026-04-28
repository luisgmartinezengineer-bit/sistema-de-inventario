package com.Luis.task_manager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountingReportResponse {

    private BigDecimal totalSubtotal;
    private BigDecimal totalTax;
    private BigDecimal totalSales;
    private long salesCount;

    private List<CajaBreakdown> byCaja;
    private List<PaymentBreakdown> byPaymentMethod;
    private List<DailyPoint> dailyTrend;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CajaBreakdown {
        private Long cajaId;
        private String cajaName;
        private String sellerName;
        private BigDecimal subtotal;
        private BigDecimal tax;
        private BigDecimal total;
        private long count;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PaymentBreakdown {
        private String method;
        private BigDecimal total;
        private long count;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class DailyPoint {
        private String date;
        private BigDecimal total;
        private long count;
    }
}
