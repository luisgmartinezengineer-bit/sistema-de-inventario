package com.Luis.task_manager.controller;

import com.Luis.task_manager.dto.StockAlertResponse;
import com.Luis.task_manager.service.StockAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class StockAlertController {

    private final StockAlertService alertService;

    @GetMapping
    public List<StockAlertResponse> findAll() {
        return alertService.findAll();
    }

    @GetMapping("/active")
    public List<StockAlertResponse> findActive() {
        return alertService.findActive();
    }

    @PatchMapping("/{id}/resolve")
    public StockAlertResponse resolve(@PathVariable Long id) {
        return alertService.resolve(id);
    }
}
