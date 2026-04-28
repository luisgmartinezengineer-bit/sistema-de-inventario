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

/**
 * Servicio encargado de gestionar las alertas de bajo inventario.
 *
 * <p>Es invocado automáticamente por {@link ProductService} y {@link SaleService}
 * cada vez que el stock de un producto cambia, ya sea por una venta o por
 * un ajuste manual.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StockAlertService {

    private final StockAlertRepository alertRepository;

    /**
     * Evalúa el stock actual del producto y crea, actualiza o resuelve alertas según corresponda.
     *
     * <ul>
     *   <li>Si {@code stock <= minStock}: crea una nueva alerta o actualiza la existente.</li>
     *   <li>Si el stock se recupera: resuelve automáticamente la alerta abierta.</li>
     * </ul>
     *
     * @param product producto cuyo stock acaba de cambiar
     */
    public void checkAndAlert(Product product) {
        boolean isLow = product.getStock() <= product.getMinStock();

        if (isLow) {
            // Si ya existe una alerta activa, solo se actualiza el stock actual.
            // Si no existe, se crea una nueva alerta.
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
            // El stock se recuperó: se cierra automáticamente cualquier alerta abierta.
            alertRepository.findByProductIdAndResolvedFalse(product.getId())
                    .ifPresent(a -> {
                        a.setResolved(true);
                        a.setResolvedAt(LocalDateTime.now());
                        alertRepository.save(a);
                    });
        }
    }

    /**
     * Retorna todas las alertas que aún no han sido resueltas, ordenadas
     * de más reciente a más antigua.
     */
    @Transactional(readOnly = true)
    public List<StockAlertResponse> findActive() {
        return alertRepository.findByResolvedFalseOrderByCreatedAtDesc().stream()
                .map(StockAlertResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Retorna el historial completo de alertas (activas y resueltas),
     * ordenadas de más reciente a más antigua.
     */
    @Transactional(readOnly = true)
    public List<StockAlertResponse> findAll() {
        return alertRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(StockAlertResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Marca manualmente una alerta como resuelta.
     *
     * @param alertId ID de la alerta a resolver
     * @return la alerta actualizada
     * @throws ResourceNotFoundException si no existe una alerta con ese ID
     */
    public StockAlertResponse resolve(Long alertId) {
        StockAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerta no encontrada: " + alertId));
        alert.setResolved(true);
        alert.setResolvedAt(LocalDateTime.now());
        return StockAlertResponse.from(alertRepository.save(alert));
    }
}
