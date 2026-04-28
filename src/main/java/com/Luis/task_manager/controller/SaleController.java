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

/**
 * Controlador REST para el registro y consulta de ventas.
 *
 * <p>Base URL: {@code /api/sales}</p>
 */
@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    /**
     * Lista ventas con filtros opcionales:
     * <ul>
     *   <li>{@code ?invoice=FV001} — búsqueda por número de factura</li>
     *   <li>{@code ?from=...&to=...} — rango de fechas en formato ISO datetime</li>
     *   <li>{@code ?cashRegisterId=1} — ventas de una caja específica</li>
     *   <li>Sin parámetros: retorna las 10 ventas más recientes</li>
     * </ul>
     */
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

    /**
     * Retorna el resumen de ventas del día actual y del mes en curso
     * (total en dinero y número de transacciones).
     */
    @GetMapping("/summary")
    public SaleSummaryResponse summary() {
        return saleService.getSummary();
    }

    /**
     * Retorna el detalle completo de una venta por su ID, incluyendo todos sus ítems.
     */
    @GetMapping("/{id}")
    public SaleResponse findById(@PathVariable Long id) {
        return saleService.findById(id);
    }

    /**
     * Registra una nueva venta en el sistema.
     * La operación es atómica: si algún producto no tiene stock suficiente,
     * toda la venta es rechazada con HTTP 409 Conflict.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleResponse create(@Valid @RequestBody SaleRequest req) {
        return saleService.create(req);
    }
}
