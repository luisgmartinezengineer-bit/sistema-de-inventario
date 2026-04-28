package com.Luis.task_manager.controller;

import com.Luis.task_manager.dto.RejectReturnRequest;
import com.Luis.task_manager.dto.ReturnRequest;
import com.Luis.task_manager.dto.ReturnResponse;
import com.Luis.task_manager.service.SaleReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class SaleReturnController {

    private final SaleReturnService returnService;

    /** ADMIN/SUPERVISOR: ver todas */
    @GetMapping
    public List<ReturnResponse> findAll() { return returnService.findAll(); }

    /** ADMIN/SUPERVISOR: ver pendientes */
    @GetMapping("/pending")
    public List<ReturnResponse> findPending() { return returnService.findPending(); }

    /** Cualquier rol autenticado: ver las propias */
    @GetMapping("/my")
    public List<ReturnResponse> findMine() { return returnService.findMine(); }

    @GetMapping("/{id}")
    public ReturnResponse findById(@PathVariable Long id) { return returnService.findById(id); }

    @GetMapping("/by-sale/{saleId}")
    public List<ReturnResponse> findBySale(@PathVariable Long saleId) { return returnService.findBySale(saleId); }

    /** Cualquier rol autenticado: solicitar devolución */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReturnResponse create(@Valid @RequestBody ReturnRequest req) { return returnService.create(req); }

    /** ADMIN/SUPERVISOR: aprobar */
    @PatchMapping("/{id}/approve")
    public ReturnResponse approve(@PathVariable Long id) { return returnService.approve(id); }

    /** ADMIN/SUPERVISOR: rechazar */
    @PatchMapping("/{id}/reject")
    public ReturnResponse reject(@PathVariable Long id, @Valid @RequestBody RejectReturnRequest req) {
        return returnService.reject(id, req.getRejectionReason());
    }
}
