package com.Luis.task_manager.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data @Builder
public class SupplierDashboardResponse {
    private int totalSuppliers;
    private int activeAlerts;
    private int buyOpportunities;
    private int marginAtRisk;
    private List<SupplierAlertResponse> criticalAlerts;
    private List<SupplierAlertResponse> opportunities;
    private List<SupplierAlertResponse> predictions;
    private List<SupplierEventResponse> upcomingEvents;
}
