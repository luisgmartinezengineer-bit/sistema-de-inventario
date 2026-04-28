package com.Luis.task_manager.service;

import com.Luis.task_manager.dto.ReturnRequest;
import com.Luis.task_manager.dto.ReturnResponse;
import com.Luis.task_manager.entity.*;
import com.Luis.task_manager.exception.ResourceNotFoundException;
import com.Luis.task_manager.repository.AppUserRepository;
import com.Luis.task_manager.repository.SaleRepository;
import com.Luis.task_manager.repository.SaleReturnRepository;
import com.Luis.task_manager.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SaleReturnService {

    private final SaleReturnRepository returnRepository;
    private final SaleRepository saleRepository;
    private final StockMovementRepository movementRepository;
    private final AppUserRepository userRepository;
    private final StockAlertService alertService;
    private final AuditService auditService;

    /** Cualquier rol puede crear una solicitud — queda en PENDIENTE, sin tocar stock */
    public ReturnResponse create(ReturnRequest req) {
        Sale sale = saleRepository.findById(req.getSaleId())
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada: " + req.getSaleId()));

        Map<Long, Integer> soldQty = sale.getItems().stream()
                .collect(Collectors.toMap(i -> i.getProduct().getId(), SaleItem::getQuantity));

        // Solo contar devolucioes aprobadas para calcular el máximo retornable
        Map<Long, Integer> alreadyReturnedQty = returnRepository.findBySaleIdOrderByRequestDateDesc(sale.getId())
                .stream()
                .filter(r -> r.getStatus() == SaleReturn.ReturnStatus.APROBADA)
                .flatMap(r -> r.getItems().stream())
                .collect(Collectors.groupingBy(
                        i -> i.getProduct().getId(),
                        Collectors.summingInt(SaleReturnItem::getQuantity)));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser requestedBy = userRepository.findByUsername(username).orElse(null);

        Map<Long, SaleItem> saleItemsByProduct = sale.getItems().stream()
                .collect(Collectors.toMap(i -> i.getProduct().getId(), i -> i));

        SaleReturn saleReturn = SaleReturn.builder()
                .sale(sale)
                .requestDate(LocalDateTime.now())
                .reason(req.getReason())
                .notes(req.getNotes())
                .requestedBy(requestedBy)
                .status(SaleReturn.ReturnStatus.PENDIENTE)
                .items(new ArrayList<>())
                .build();

        saleReturn = returnRepository.save(saleReturn);

        BigDecimal total = BigDecimal.ZERO;

        for (var itemReq : req.getItems()) {
            Long productId = itemReq.getProductId();
            SaleItem originalItem = saleItemsByProduct.get(productId);
            if (originalItem == null)
                throw new IllegalArgumentException("El producto " + productId + " no pertenece a esta venta");

            int maxReturnable = soldQty.getOrDefault(productId, 0)
                    - alreadyReturnedQty.getOrDefault(productId, 0);
            if (itemReq.getQuantity() > maxReturnable)
                throw new IllegalArgumentException(
                        "Cantidad (" + itemReq.getQuantity() + ") supera lo disponible (" + maxReturnable + ") para: " + originalItem.getProduct().getName());

            BigDecimal unitPrice = originalItem.getUnitPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            SaleReturnItem returnItem = SaleReturnItem.builder()
                    .saleReturn(saleReturn)
                    .product(originalItem.getProduct())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();

            saleReturn.getItems().add(returnItem);
            total = total.add(subtotal);
        }

        saleReturn.setRefundTotal(total);
        ReturnResponse saved = ReturnResponse.from(returnRepository.save(saleReturn));
        auditService.log("DEVOLUCION_SOLICITADA", "Devolucion", saved.getId(),
                "Factura: " + saved.getInvoiceNumber() + " | Monto: " + saved.getRefundTotal());
        return saved;
    }

    /** Solo SUPERVISOR o ADMIN — aprueba y restaura el stock */
    public ReturnResponse approve(Long id) {
        SaleReturn saleReturn = returnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Devolución no encontrada: " + id));

        if (saleReturn.getStatus() != SaleReturn.ReturnStatus.PENDIENTE)
            throw new IllegalArgumentException("Solo se pueden aprobar solicitudes en estado PENDIENTE");

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser processedBy = userRepository.findByUsername(username).orElse(null);

        List<StockMovement> movements = new ArrayList<>();
        Sale sale = saleReturn.getSale();

        for (SaleReturnItem item : saleReturn.getItems()) {
            Product product = item.getProduct();
            int before = product.getStock();
            product.setStock(before + item.getQuantity());
            movements.add(StockMovement.builder()
                    .product(product)
                    .type(StockMovement.MovementType.ENTRY)
                    .quantity(item.getQuantity())
                    .stockBefore(before)
                    .stockAfter(product.getStock())
                    .date(LocalDateTime.now())
                    .reason("Devolución aprobada — " + (sale.getInvoiceNumber() != null ? sale.getInvoiceNumber() : "venta #" + sale.getId()))
                    .saleId(sale.getId())
                    .build());
        }

        movementRepository.saveAll(movements);
        for (SaleReturnItem item : saleReturn.getItems()) alertService.checkAndAlert(item.getProduct());

        saleReturn.setStatus(SaleReturn.ReturnStatus.APROBADA);
        saleReturn.setProcessedBy(processedBy);
        saleReturn.setProcessedDate(LocalDateTime.now());

        ReturnResponse result = ReturnResponse.from(returnRepository.save(saleReturn));
        auditService.log("DEVOLUCION_APROBADA", "Devolucion", result.getId(),
                "Factura: " + result.getInvoiceNumber() + " | Monto: " + result.getRefundTotal());
        return result;
    }

    /** Solo SUPERVISOR o ADMIN — rechaza la solicitud */
    public ReturnResponse reject(Long id, String rejectionReason) {
        SaleReturn saleReturn = returnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Devolución no encontrada: " + id));

        if (saleReturn.getStatus() != SaleReturn.ReturnStatus.PENDIENTE)
            throw new IllegalArgumentException("Solo se pueden rechazar solicitudes en estado PENDIENTE");

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser processedBy = userRepository.findByUsername(username).orElse(null);

        saleReturn.setStatus(SaleReturn.ReturnStatus.RECHAZADA);
        saleReturn.setProcessedBy(processedBy);
        saleReturn.setProcessedDate(LocalDateTime.now());
        saleReturn.setRejectionReason(rejectionReason);

        ReturnResponse result = ReturnResponse.from(returnRepository.save(saleReturn));
        auditService.log("DEVOLUCION_RECHAZADA", "Devolucion", result.getId(),
                "Motivo: " + rejectionReason);
        return result;
    }

    @Transactional(readOnly = true)
    public List<ReturnResponse> findAll() {
        return returnRepository.findAllByOrderByRequestDateDesc().stream()
                .map(ReturnResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReturnResponse> findPending() {
        return returnRepository.findByStatusOrderByRequestDateAsc(SaleReturn.ReturnStatus.PENDIENTE).stream()
                .map(ReturnResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReturnResponse> findMine() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return returnRepository.findByRequestedByUsernameOrderByRequestDateDesc(username).stream()
                .map(ReturnResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReturnResponse> findBySale(Long saleId) {
        return returnRepository.findBySaleIdOrderByRequestDateDesc(saleId).stream()
                .map(ReturnResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReturnResponse findById(Long id) {
        return ReturnResponse.from(returnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Devolución no encontrada: " + id)));
    }
}
