package com.Luis.task_manager.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data @Builder
public class PriceAnalysisResponse {
    private Long supplierProductId;
    private String supplierName;
    private String productName;
    private BigDecimal currentPrice;
    private BigDecimal salePrice;       // precio de venta actual del producto
    private BigDecimal marginPercent;   // (salePrice - currentPrice) / salePrice * 100

    // Estadísticos
    private BigDecimal ma7;             // promedio móvil 7 cotizaciones
    private BigDecimal ma30;            // promedio móvil 30 cotizaciones
    private BigDecimal minHistorical;
    private BigDecimal maxHistorical;
    private BigDecimal volatility;      // desviación estándar

    // Tendencia
    private String trend;               // SUBIDA | ESTABLE | BAJADA
    private BigDecimal trendPercent;    // % de cambio últimas 2 cotizaciones

    // Predicción
    private BigDecimal predictedPrice30Days;
    private BigDecimal predictedChangePercent;
    private String predictionConfidence; // BAJA | MEDIA | ALTA
    private int quotesUsedForPrediction;

    // Flags
    private boolean isBuyOpportunity;   // precio cerca del mínimo histórico
    private boolean isMarginAtRisk;     // margen < 20%
    private boolean isStale;            // sin cotizar > 45 días

    // Score de proveedor
    private double score;               // 0-100
    private double priceScore;
    private double reliabilityScore;
    private double speedScore;
    private double termsScore;

    // Historial reciente para la gráfica
    private List<PricePointDTO> priceHistory;

    @Data @Builder
    public static class PricePointDTO {
        private String date;
        private BigDecimal price;
        private BigDecimal variationPercent;
    }
}
