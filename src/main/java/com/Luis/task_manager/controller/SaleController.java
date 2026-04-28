package com.Luis.task_manager.controller;

import com.Luis.task_manager.dto.SaleRequest;
import com.Luis.task_manager.dto.SaleResponse;
import com.Luis.task_manager.dto.SaleSummaryResponse;
import com.Luis.task_manager.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @GetMapping
    public List<SaleResponse> findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Long cashRegisterId,
            @RequestParam(required = false) String invoice) {
        if (invoice != null && !invoice.isBlank()) return saleService.searchByInvoice(invoice);
        if (from != null && to != null) return saleService.findByDateRange(from, to);
        if (cashRegisterId != null) return saleService.findByCashRegister(cashRegisterId);
        return saleService.findAll();
    }

    @GetMapping("/summary")
    public SaleSummaryResponse summary() {
        return saleService.getSummary();
    }

    @GetMapping("/{id}")
    public SaleResponse findById(@PathVariable Long id) {
        return saleService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleResponse create(@Valid @RequestBody SaleRequest req) {
        return saleService.create(req);
    }
}
