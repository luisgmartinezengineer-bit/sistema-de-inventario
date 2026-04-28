package com.Luis.task_manager.controller;

import com.Luis.task_manager.dto.*;
import com.Luis.task_manager.service.SupplierAlertService;
import com.Luis.task_manager.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;
    private final SupplierAlertService alertService;

    // ── Dashboard ─────────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public SupplierDashboardResponse getDashboard() {
        return supplierService.getDashboard();
    }

    // ── Suppliers ─────────────────────────────────────────────────────────
    @GetMapping
    public List<SupplierResponse> findAll() { return supplierService.findAll(); }

    @GetMapping("/{id}")
    public SupplierResponse findById(@PathVariable Long id) { return supplierService.findById(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierResponse create(@Valid @RequestBody SupplierRequest req) { return supplierService.create(req); }

    @PutMapping("/{id}")
    public SupplierResponse update(@PathVariable Long id, @Valid @RequestBody SupplierRequest req) {
        return supplierService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) { supplierService.deactivate(id); }

    // ── Supplier Products ─────────────────────────────────────────────────
    @GetMapping("/{supplierId}/products")
    public List<SupplierProductResponse> findProductsBySupplier(@PathVariable Long supplierId) {
        return supplierService.findProductsBySupplier(supplierId);
    }

    @GetMapping("/by-product/{productId}")
    public List<SupplierProductResponse> findSuppliersByProduct(@PathVariable Long productId) {
        return supplierService.findSuppliersByProduct(productId);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierProductResponse addSupplierProduct(@Valid @RequestBody SupplierProductRequest req) {
        return supplierService.addSupplierProduct(req);
    }

    @DeleteMapping("/products/{spId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateSupplierProduct(@PathVariable Long spId) {
        supplierService.deactivateSupplierProduct(spId);
    }

    // ── Price Quotes ──────────────────────────────────────────────────────
    @GetMapping("/products/{spId}/quotes")
    public List<PriceQuoteResponse> findQuotes(@PathVariable Long spId) {
        return supplierService.findQuotes(spId);
    }

    @PostMapping("/products/{spId}/quotes")
    @ResponseStatus(HttpStatus.CREATED)
    public PriceQuoteResponse addQuote(@PathVariable Long spId, @Valid @RequestBody PriceQuoteRequest req) {
        return supplierService.addQuote(spId, req);
    }

    // ── Price Analysis ────────────────────────────────────────────────────
    @GetMapping("/products/{spId}/analysis")
    public PriceAnalysisResponse analyze(@PathVariable Long spId) {
        return supplierService.analyze(spId);
    }

    @GetMapping("/compare/{productId}")
    public List<PriceAnalysisResponse> compareSuppliers(@PathVariable Long productId) {
        return supplierService.compareSuppliers(productId);
    }

    // ── Events ────────────────────────────────────────────────────────────
    @GetMapping("/events")
    public List<SupplierEventResponse> findEvents() { return supplierService.findEvents(); }

    @GetMapping("/events/upcoming")
    public List<SupplierEventResponse> findUpcomingEvents() { return supplierService.findUpcomingEvents(); }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierEventResponse createEvent(@Valid @RequestBody SupplierEventRequest req) {
        return supplierService.createEvent(req);
    }

    // ── Alerts ────────────────────────────────────────────────────────────
    @GetMapping("/alerts")
    public List<SupplierAlertResponse> findActiveAlerts() { return supplierService.findActiveAlerts(); }

    @GetMapping("/alerts/all")
    public List<SupplierAlertResponse> findAllAlerts() { return supplierService.findAllAlerts(); }

    @PatchMapping("/alerts/{id}/resolve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resolveAlert(@PathVariable Long id) { alertService.resolve(id); }
}
