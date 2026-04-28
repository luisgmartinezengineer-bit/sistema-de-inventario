package com.Luis.task_manager.service;

import com.Luis.task_manager.dto.PriceAnalysisResponse;
import com.Luis.task_manager.entity.SupplierAlert;
import com.Luis.task_manager.entity.SupplierProduct;
import com.Luis.task_manager.repository.SupplierAlertRepository;
import com.Luis.task_manager.repository.SupplierProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SupplierAlertService {

    private final SupplierAlertRepository alertRepository;
    private final SupplierProductRepository supplierProductRepository;

    private static final int COOLDOWN_DAYS = 7;

    public void checkAndAlert(SupplierProduct sp, PriceAnalysisResponse analysis) {
        Long spId = sp.getId();

        // Subida brusca de precio (> 5% respecto a cotización anterior)
        if (analysis.getTrendPercent() != null
                && analysis.getTrendPercent().compareTo(BigDecimal.valueOf(5)) > 0) {
            createIfAbsent(spId, "SUBIDA_PRECIO",
                    String.format("Precio subió %.2f%% respecto a la cotización anterior", analysis.getTrendPercent()),
                    analysis.getTrendPercent());
        }

        // Tendencia alcista (MA7 > MA30)
        if (analysis.getMa7() != null && analysis.getMa30() != null
                && analysis.getMa7().compareTo(BigDecimal.ZERO) > 0
                && analysis.getMa7().compareTo(analysis.getMa30()) > 0) {
            createIfAbsent(spId, "TENDENCIA_ALCISTA",
                    String.format("Promedio 7 días ($%.2f) supera promedio 30 días ($%.2f) — tendencia alcista",
                            analysis.getMa7(), analysis.getMa30()),
                    analysis.getMa7());
        }

        // Precio cerca del mínimo histórico → oportunidad de compra
        if (analysis.isBuyOpportunity()) {
            createIfAbsent(spId, "MINIMO_HISTORICO",
                    String.format("Precio actual ($%.2f) está cerca del mínimo histórico ($%.2f) — oportunidad de compra",
                            analysis.getCurrentPrice(), analysis.getMinHistorical()),
                    analysis.getCurrentPrice());
        }

        // Predicción de subida > 8% con confianza ALTA
        if ("ALTA".equals(analysis.getPredictionConfidence())
                && analysis.getPredictedChangePercent() != null
                && analysis.getPredictedChangePercent().compareTo(BigDecimal.valueOf(8)) > 0) {
            LocalDate estimatedDate = LocalDate.now().plusDays(30);
            createIfAbsent(spId, "PREDICCION_SUBIDA",
                    String.format("Modelo predice subida de %.2f%% en 30 días (precio estimado $%.2f) — confianza ALTA",
                            analysis.getPredictedChangePercent(), analysis.getPredictedPrice30Days()),
                    analysis.getPredictedChangePercent(),
                    estimatedDate);
        }

        // Sin cotizar hace más de 45 días
        if (analysis.isStale()) {
            createIfAbsent(spId, "SIN_COTIZAR",
                    "No se registra cotización en más de 45 días — actualizar precio con proveedor",
                    null);
        }

        // Margen en riesgo (< 20%)
        if (analysis.isMarginAtRisk()) {
            createIfAbsent(spId, "MARGEN_EN_RIESGO",
                    String.format("Margen actual %.2f%% está por debajo del umbral mínimo (20%%)",
                            analysis.getMarginPercent()),
                    analysis.getMarginPercent());
        }
    }

    private void createIfAbsent(Long spId, String type, String message, BigDecimal refValue) {
        createIfAbsent(spId, type, message, refValue, null);
    }

    private void createIfAbsent(Long spId, String type, String message, BigDecimal refValue, LocalDate estimatedDate) {
        LocalDateTime cooldownStart = LocalDateTime.now().minusDays(COOLDOWN_DAYS);
        boolean exists = alertRepository.existsBySupplierProductIdAndTypeAndResolvedFalseAndCreatedAtAfter(
                spId, type, cooldownStart);
        if (exists) return;

        SupplierProduct sp = supplierProductRepository.getReferenceById(spId);

        alertRepository.save(SupplierAlert.builder()
                .supplierProduct(sp)
                .type(type)
                .message(message)
                .referenceValue(refValue)
                .estimatedEventDate(estimatedDate)
                .build());
    }

    public void resolve(Long alertId) {
        SupplierAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new com.Luis.task_manager.exception.ResourceNotFoundException("Alerta no encontrada: " + alertId));
        alert.setResolved(true);
        alertRepository.save(alert);
    }
}

