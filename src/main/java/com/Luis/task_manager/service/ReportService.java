package com.Luis.task_manager.service;

import com.Luis.task_manager.dto.AccountingReportResponse;
import com.Luis.task_manager.dto.AccountingReportResponse.*;
import com.Luis.task_manager.entity.Sale;
import com.Luis.task_manager.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final SaleRepository saleRepository;
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public AccountingReportResponse getAccounting(LocalDate from, LocalDate to, Long cashRegisterId) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt   = to.plusDays(1).atStartOfDay();

        List<Sale> sales = cashRegisterId != null
                ? saleRepository.findByCashRegisterIdAndDateBetweenOrderByDateDesc(cashRegisterId, fromDt, toDt)
                : saleRepository.findByDateBetweenOrderByDateDesc(fromDt, toDt);

        BigDecimal totalSubtotal = sales.stream().map(s -> s.getSubtotal() != null ? s.getSubtotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTax = sales.stream().map(s -> s.getTaxAmount() != null ? s.getTaxAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSales = sales.stream().map(s -> s.getTotal() != null ? s.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Por caja
        Map<Long, List<Sale>> byCajaMap = sales.stream()
                .filter(s -> s.getCashRegister() != null)
                .collect(Collectors.groupingBy(s -> s.getCashRegister().getId()));

        List<CajaBreakdown> byCaja = byCajaMap.entrySet().stream().map(e -> {
            List<Sale> cs = e.getValue();
            Sale first = cs.get(0);
            return new CajaBreakdown(
                    e.getKey(),
                    first.getCashRegister().getName(),
                    first.getCashRegister().getSeller() != null ? first.getCashRegister().getSeller().getFullName() : "—",
                    cs.stream().map(s -> s.getSubtotal() != null ? s.getSubtotal() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add),
                    cs.stream().map(s -> s.getTaxAmount() != null ? s.getTaxAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add),
                    cs.stream().map(s -> s.getTotal() != null ? s.getTotal() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add),
                    cs.size()
            );
        }).sorted(Comparator.comparing(CajaBreakdown::getTotal).reversed()).collect(Collectors.toList());

        // Sin caja asignada
        List<Sale> sinCaja = sales.stream().filter(s -> s.getCashRegister() == null).collect(Collectors.toList());
        if (!sinCaja.isEmpty()) {
            byCaja.add(new CajaBreakdown(null, "Sin caja", "—",
                    sinCaja.stream().map(s -> s.getSubtotal() != null ? s.getSubtotal() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add),
                    sinCaja.stream().map(s -> s.getTaxAmount() != null ? s.getTaxAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add),
                    sinCaja.stream().map(s -> s.getTotal() != null ? s.getTotal() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add),
                    sinCaja.size()));
        }

        // Por método de pago
        Map<String, List<Sale>> byPm = sales.stream()
                .collect(Collectors.groupingBy(s -> s.getPaymentMethod() != null ? s.getPaymentMethod().name() : "EFECTIVO"));

        List<PaymentBreakdown> byPayment = byPm.entrySet().stream().map(e ->
                new PaymentBreakdown(e.getKey(),
                        e.getValue().stream().map(s -> s.getTotal() != null ? s.getTotal() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add),
                        e.getValue().size())
        ).sorted(Comparator.comparing(PaymentBreakdown::getTotal).reversed()).collect(Collectors.toList());

        // Tendencia diaria
        Map<LocalDate, List<Sale>> byDay = sales.stream()
                .collect(Collectors.groupingBy(s -> s.getDate().toLocalDate()));

        List<DailyPoint> daily = byDay.entrySet().stream().map(e ->
                new DailyPoint(e.getKey().format(DAY_FMT),
                        e.getValue().stream().map(s -> s.getTotal() != null ? s.getTotal() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add),
                        e.getValue().size())
        ).sorted(Comparator.comparing(DailyPoint::getDate)).collect(Collectors.toList());

        return new AccountingReportResponse(totalSubtotal, totalTax, totalSales, sales.size(), byCaja, byPayment, daily);
    }
}
