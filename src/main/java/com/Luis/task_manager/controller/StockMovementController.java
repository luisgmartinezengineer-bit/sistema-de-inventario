package com.Luis.task_manager.controller;

import com.Luis.task_manager.dto.StockMovementResponse;
import com.Luis.task_manager.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/movements")
@RequiredArgsConstructor
public class StockMovementController {

    private final StockMovementRepository movementRepository;

    @GetMapping
    public List<StockMovementResponse> findRecent() {
        return movementRepository.findTop50ByOrderByDateDesc().stream()
                .map(StockMovementResponse::from)
                .collect(Collectors.toList());
    }
}
