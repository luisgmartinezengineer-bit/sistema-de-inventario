package com.Luis.task_manager.service;

import com.Luis.task_manager.dto.StockAlertResponse;
import com.Luis.task_manager.entity.Product;
import com.Luis.task_manager.entity.StockAlert;
import com.Luis.task_manager.exception.ResourceNotFoundException;
import com.Luis.task_manager.repository.StockAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StockAlertService {

    private final StockAlertRepository alertRepository;

    public void checkAndAlert(Product product) {
        boolean isLow = product.getStock() <= product.getMinStock();

        if (isLow) {
            alertRepository.findByProductIdAndResolvedFalse(product.getId())
                    .ifPresentOrElse(
                            existing -> {
                                existing.setCurrentStock(product.getStock());
                                alertRepository.save(existing);
                            },
                            () -> alertRepository.save(StockAlert.builder()
                                    .product(product)
                                    .currentStock(product.getStock())
                                    .minStock(product.getMinStock())
                                    .createdAt(LocalDateTime.now())
                                    .resolved(false)
                                    .build())
                    );
        } else {
            // stock recovered — auto-resolve any open alert
            alertRepository.findByProductIdAndResolvedFalse(product.getId())
                    .ifPresent(a -> {
                        a.setResolved(true);
                        a.setResolvedAt(LocalDateTime.now());
                        alertRepository.save(a);
                    });
        }
    }

    @Transactional(readOnly = true)
    public List<StockAlertResponse> findActive() {
        return alertRepository.findByResolvedFalseOrderByCreatedAtDesc().stream()
                .map(StockAlertResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockAlertResponse> findAll() {
        return alertRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(StockAlertResponse::from)
                .collect(Collectors.toList());
    }

    public StockAlertResponse resolve(Long alertId) {
        StockAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerta no encontrada: " + alertId));
        alert.setResolved(true);
        alert.setResolvedAt(LocalDateTime.now());
        return StockAlertResponse.from(alertRepository.save(alert));
    }
}
