package com.Luis.task_manager.service;

import com.Luis.task_manager.dto.PurchaseOrderRequest;
import com.Luis.task_manager.dto.PurchaseOrderResponse;
import com.Luis.task_manager.entity.*;
import com.Luis.task_manager.exception.ResourceNotFoundException;
import com.Luis.task_manager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseOrderService {

    private final PurchaseOrderRepository orderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;
    private final AppUserRepository userRepository;
    private final AuditService auditService;

    public PurchaseOrderResponse create(PurchaseOrderRequest req) {
        Supplier supplier = req.getSupplierId() != null
                ? supplierRepository.findById(req.getSupplierId())
                        .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + req.getSupplierId()))
                : null;

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser createdBy = userRepository.findByUsername(username).orElse(null);

        String orderNumber = "OC-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());

        PurchaseOrder order = PurchaseOrder.builder()
                .orderNumber(orderNumber)
                .supplier(supplier)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .expectedDeliveryDate(req.getExpectedDeliveryDate())
                .notes(req.getNotes())
                .status(PurchaseOrder.OrderStatus.PENDIENTE)
                .items(new ArrayList<>())
                .build();

        order = orderRepository.save(order);

        BigDecimal total = BigDecimal.ZERO;
        for (var itemReq : req.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + itemReq.getProductId()));

            BigDecimal subtotal = itemReq.getUnitCost().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .purchaseOrder(order)
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .unitCost(itemReq.getUnitCost())
                    .subtotal(subtotal)
                    .build();
            order.getItems().add(item);
            total = total.add(subtotal);
        }

        order.setTotal(total);
        PurchaseOrderResponse saved = PurchaseOrderResponse.from(orderRepository.save(order));
        auditService.log("COMPRA_CREADA", "OrdenCompra", saved.getId(), "Orden: " + orderNumber + " | Total: " + total);
        return saved;
    }

    /** Marca la orden como RECIBIDA y suma stock a los productos */
    public PurchaseOrderResponse receive(Long id) {
        PurchaseOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada: " + id));

        if (order.getStatus() == PurchaseOrder.OrderStatus.RECIBIDA)
            throw new IllegalArgumentException("La orden ya fue recibida");
        if (order.getStatus() == PurchaseOrder.OrderStatus.CANCELADA)
            throw new IllegalArgumentException("No se puede recibir una orden cancelada");

        List<StockMovement> movements = new ArrayList<>();
        for (PurchaseOrderItem item : order.getItems()) {
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
                    .reason("Recepción orden " + order.getOrderNumber())
                    .build());
        }
        movementRepository.saveAll(movements);
        order.setStatus(PurchaseOrder.OrderStatus.RECIBIDA);

        PurchaseOrderResponse result = PurchaseOrderResponse.from(orderRepository.save(order));
        auditService.log("COMPRA_RECIBIDA", "OrdenCompra", result.getId(), "Orden: " + order.getOrderNumber());
        return result;
    }

    public PurchaseOrderResponse cancel(Long id) {
        PurchaseOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada: " + id));
        if (order.getStatus() == PurchaseOrder.OrderStatus.RECIBIDA)
            throw new IllegalArgumentException("No se puede cancelar una orden ya recibida");
        order.setStatus(PurchaseOrder.OrderStatus.CANCELADA);
        PurchaseOrderResponse result = PurchaseOrderResponse.from(orderRepository.save(order));
        auditService.log("COMPRA_CANCELADA", "OrdenCompra", result.getId(), "Orden: " + order.getOrderNumber());
        return result;
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> findAll() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(PurchaseOrderResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse findById(Long id) {
        return PurchaseOrderResponse.from(orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada: " + id)));
    }
}
