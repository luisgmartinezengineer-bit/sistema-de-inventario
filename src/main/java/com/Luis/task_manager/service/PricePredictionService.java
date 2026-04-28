package com.Luis.task_manager.service;

import com.Luis.task_manager.dto.PriceAnalysisResponse;
import com.Luis.task_manager.dto.PriceAnalysisResponse.PricePointDTO;
import com.Luis.task_manager.entity.PriceQuote;
import com.Luis.task_manager.entity.SupplierProduct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PricePredictionService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int STALE_DAYS = 45;
    private static final double BUY_OPPORTUNITY_THRESHOLD = 1.05; // dentro del 5% del mínimo
    private static final double MARGIN_RISK_THRESHOLD = 0.20;     // margen < 20%

    public PriceAnalysisResponse analyze(SupplierProduct sp, List<PriceQuote> quotes) {
        List<PriceQuote> valid = quotes.stream().filter(PriceQuote::isValid).collect(Collectors.toList());

        BigDecimal current = sp.getCurrentPrice() != null ? sp.getCurrentPrice() : BigDecimal.ZERO;
        BigDecimal salePrice = sp.getProduct().getPrice();
        BigDecimal min = sp.getMinHistoricalPrice();
        BigDecimal max = sp.getMaxHistoricalPrice();

        // Margen
        BigDecimal margin = BigDecimal.ZERO;
        if (salePrice != null && salePrice.compareTo(BigDecimal.ZERO) > 0 && current.compareTo(BigDecimal.ZERO) > 0)
            margin = salePrice.subtract(current).divide(salePrice, 4, RoundingMode.HALF_UP).multiply(HUNDRED);

        // Promedio móvil
        BigDecimal ma7  = movingAverage(valid, 7);
        BigDecimal ma30 = movingAverage(valid, 30);

        // Tendencia
        String trend = "ESTABLE";
        BigDecimal trendPct = BigDecimal.ZERO;
        if (valid.size() >= 2) {
            BigDecimal prev = valid.get(1).getPrice();
            if (prev.compareTo(BigDecimal.ZERO) > 0) {
                trendPct = current.subtract(prev).divide(prev, 4, RoundingMode.HALF_UP).multiply(HUNDRED);
                if (trendPct.compareTo(BigDecimal.valueOf(2)) > 0)  trend = "SUBIDA";
                else if (trendPct.compareTo(BigDecimal.valueOf(-2)) < 0) trend = "BAJADA";
            }
        }

        // Volatilidad (desviación estándar)
        BigDecimal volatility = standardDeviation(valid);

        // Predicción por regresión lineal
        PredictionResult pred = linearRegression(valid);

        // Flags
        boolean buyOpp = min != null && current.compareTo(BigDecimal.ZERO) > 0
                && current.compareTo(min.multiply(BigDecimal.valueOf(BUY_OPPORTUNITY_THRESHOLD))) <= 0
                && sp.getProduct().getStock() < sp.getProduct().getMinStock() * 2;

        boolean marginRisk = margin.compareTo(BigDecimal.valueOf(MARGIN_RISK_THRESHOLD * 100)) < 0
                && current.compareTo(BigDecimal.ZERO) > 0;

        boolean stale = sp.getLastQuoteDate() == null ||
                sp.getLastQuoteDate().isBefore(java.time.LocalDateTime.now().minusDays(STALE_DAYS));

        // Historial para gráfica
        List<PricePointDTO> history = valid.stream().limit(30)
                .map(q -> PricePointDTO.builder()
                        .date(q.getDate().format(FMT))
                        .price(q.getPrice())
                        .variationPercent(q.getVariationPercent())
                        .build())
                .collect(Collectors.toList());
        java.util.Collections.reverse(history); // cronológico

        return PriceAnalysisResponse.builder()
                .supplierProductId(sp.getId())
                .supplierName(sp.getSupplier().getName())
                .productName(sp.getProduct().getName())
                .currentPrice(current)
                .salePrice(salePrice)
                .marginPercent(margin.setScale(2, RoundingMode.HALF_UP))
                .ma7(ma7).ma30(ma30)
                .minHistorical(min).maxHistorical(max)
                .volatility(volatility)
                .trend(trend).trendPercent(trendPct.setScale(2, RoundingMode.HALF_UP))
                .predictedPrice30Days(pred.price)
                .predictedChangePercent(pred.changePercent)
                .predictionConfidence(pred.confidence)
                .quotesUsedForPrediction(valid.size())
                .isBuyOpportunity(buyOpp)
                .isMarginAtRisk(marginRisk)
                .isStale(stale)
                .priceHistory(history)
                .build();
    }

    // ── Score de proveedor (0-100) ────────────────────────────────────────
    public double calculateScore(SupplierProduct sp, List<PriceQuote> quotes,
                                 BigDecimal bestPriceInMarket, int bestLeadTime, int maxPaymentDays) {
        double priceScore = 0, reliabilityScore = 0, speedScore = 0, termsScore = 0;

        BigDecimal current = sp.getCurrentPrice();
        if (current != null && current.compareTo(BigDecimal.ZERO) > 0
                && bestPriceInMarket != null && bestPriceInMarket.compareTo(BigDecimal.ZERO) > 0) {
            priceScore = bestPriceInMarket.divide(current, 4, RoundingMode.HALF_UP).doubleValue() * 100;
            priceScore = Math.min(priceScore, 100);
        }

        // Confiabilidad: basada en frecuencia de cotizaciones y volatilidad baja
        if (!quotes.isEmpty()) {
            int freq = Math.min(quotes.size() * 10, 60); // hasta 60 pts por frecuencia
            double vol = standardDeviation(quotes).doubleValue();
            double volScore = vol == 0 ? 40 : Math.max(0, 40 - vol * 2);
            reliabilityScore = freq + volScore;
            reliabilityScore = Math.min(reliabilityScore, 100);
        }

        if (bestLeadTime > 0 && sp.getSupplier().getLeadTimeDays() > 0)
            speedScore = Math.min(100, (double) bestLeadTime / sp.getSupplier().getLeadTimeDays() * 100);

        if (maxPaymentDays > 0)
            termsScore = Math.min(100, (double) sp.getSupplier().getPaymentTermsDays() / maxPaymentDays * 100);

        return priceScore * 0.40 + reliabilityScore * 0.30 + speedScore * 0.20 + termsScore * 0.10;
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private BigDecimal movingAverage(List<PriceQuote> quotes, int n) {
        List<PriceQuote> sub = quotes.stream().limit(n).collect(Collectors.toList());
        if (sub.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = sub.stream().map(PriceQuote::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(sub.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal standardDeviation(List<PriceQuote> quotes) {
        if (quotes.size() < 2) return BigDecimal.ZERO;
        double mean = quotes.stream().mapToDouble(q -> q.getPrice().doubleValue()).average().orElse(0);
        double variance = quotes.stream()
                .mapToDouble(q -> Math.pow(q.getPrice().doubleValue() - mean, 2))
                .average().orElse(0);
        return BigDecimal.valueOf(Math.sqrt(variance)).setScale(2, RoundingMode.HALF_UP);
    }

    private PredictionResult linearRegression(List<PriceQuote> quotes) {
        int n = quotes.size();
        if (n < 3) return new PredictionResult(BigDecimal.ZERO, BigDecimal.ZERO, "BAJA");

        // x = índice (0..n-1 en orden cronológico), y = precio
        List<PriceQuote> ordered = new java.util.ArrayList<>(quotes);
        java.util.Collections.reverse(ordered);

        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            double x = i, y = ordered.get(i).getPrice().doubleValue();
            sumX += x; sumY += y; sumXY += x * y; sumX2 += x * x;
        }
        double denom = n * sumX2 - sumX * sumX;
        if (Math.abs(denom) < 1e-10) return new PredictionResult(BigDecimal.ZERO, BigDecimal.ZERO, "BAJA");

        double slope = (n * sumXY - sumX * sumY) / denom;
        double intercept = (sumY - slope * sumX) / n;

        // Calcular R² para confianza
        double meanY = sumY / n;
        double ssTot = ordered.stream().mapToDouble(q -> Math.pow(q.getPrice().doubleValue() - meanY, 2)).sum();
        double ssRes = 0;
        for (int i = 0; i < n; i++) ssRes += Math.pow(ordered.get(i).getPrice().doubleValue() - (intercept + slope * i), 2);
        double r2 = ssTot > 0 ? 1 - ssRes / ssTot : 0;

        // Proyectar ~30 días: asumimos frecuencia de cotización promedio
        double predicted = intercept + slope * (n + 4); // ~4 cotizaciones más en 30 días
        double current = ordered.isEmpty() ? 0 : ordered.get(n - 1).getPrice().doubleValue();
        double changePercent = current > 0 ? (predicted - current) / current * 100 : 0;

        String confidence = r2 > 0.7 ? "ALTA" : r2 > 0.4 ? "MEDIA" : "BAJA";

        return new PredictionResult(
                BigDecimal.valueOf(Math.max(0, predicted)).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(changePercent).setScale(2, RoundingMode.HALF_UP),
                confidence);
    }

    private record PredictionResult(BigDecimal price, BigDecimal changePercent, String confidence) {}
}
