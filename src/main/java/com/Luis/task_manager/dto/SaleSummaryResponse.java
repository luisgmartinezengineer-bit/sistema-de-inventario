package com.Luis.task_manager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class SaleSummaryResponse {
    private BigDecimal totalToday;
    private BigDecimal totalThisMonth;
    private long salesCountToday;
    private long salesCountThisMonth;
}
