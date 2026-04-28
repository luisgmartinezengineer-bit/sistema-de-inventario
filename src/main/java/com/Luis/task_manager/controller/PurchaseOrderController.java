package com.Luis.task_manager.controller;

import com.Luis.task_manager.dto.PurchaseOrderRequest;
import com.Luis.task_manager.dto.PurchaseOrderResponse;
import com.Luis.task_manager.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    public List<PurchaseOrderResponse> findAll() {
        return purchaseOrderService.findAll();
    }

    @GetMapping("/{id}")
    public PurchaseOrderResponse findById(@PathVariable Long id) {
        return purchaseOrderService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseOrderResponse create(@Valid @RequestBody PurchaseOrderRequest req) {
        return purchaseOrderService.create(req);
    }

    @PatchMapping("/{id}/receive")
    public PurchaseOrderResponse receive(@PathVariable Long id) {
        return purchaseOrderService.receive(id);
    }

    @PatchMapping("/{id}/cancel")
    public PurchaseOrderResponse cancel(@PathVariable Long id) {
        return purchaseOrderService.cancel(id);
    }
}
