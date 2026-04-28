package com.Luis.task_manager.controller;

import com.Luis.task_manager.dto.AnalyticsResponse;
import com.Luis.task_manager.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    public AnalyticsResponse getAnalytics(@RequestParam(defaultValue = "30") int days) {
        return analyticsService.getAnalytics(days);
    }
}
