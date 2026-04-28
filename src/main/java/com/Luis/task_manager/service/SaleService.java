package com.Luis.task_manager.service;

import com.Luis.task_manager.dto.SaleRequest;
import com.Luis.task_manager.dto.SaleResponse;
import com.Luis.task_manager.dto.SaleSummaryResponse;
import com.Luis.task_manager.entity.*;
import com.Luis.task_manager.exception.InsufficientStockException;
import com.Luis.task_manager.exception.ResourceNotFoundException;
import com.Luis.task_manager.repository.AppUserRepository;
import com.Luis.task_manager.repository.SaleRepository;
import com.Luis.task_manager.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SaleService {

    private final SaleRepository saleRepository;
    private final StockMovementRepository movementRepository;
    private final ProductService productService;
    private final StockAlertService alertService;
    private final CustomerService customerService;
    private final CashRegisterService cashRegisterService;
    private final AppUserRepository userRepository;
    private final CompanyConfigService companyConfigService;
    private final AuditService auditService;

    public SaleResponse create(SaleRequest req) {
        Customer customer = req.getCustomerId() != null
                ? customerService.getOrThrow(req.getCustomerId()) : null;

        CashRegister cashRegister = req.getCashRegisterId() != null
                ? cashRegisterService.getOrThrow(req.getCashRegisterId()) : null;

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser seller = userRepository.findByUsername(username).orElse(null);

        Sale.PaymentMethod pm = Sale.PaymentMethod.EFECTIVO;
        if (req.getPaymentMethod() != null) {
            try { pm = Sale.PaymentMethod.valueOf(req.getPaymentMethod()); } catch (IllegalArgumentException e) {
                log.warn("Método de pago desconocido '{}', se usará EFECTIVO por defecto", req.getPaymentMethod());
            }
        }

        String invoiceNumber = companyConfigService.generateNextInvoiceNumber();

        Sale sale = Sale.builder()
                .invoiceNumber(invoiceNumber)
                .date(LocalDateTime.now())
                .customer(customer)
                .cashRegister(cashRegister)
                .seller(seller)
                .paymentMethod(pm)
                .notes(req.getNotes())
                .subtotal(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        sale = saleRepository.save(sale);

        BigDecimal totalSubtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        List<StockMovement> movements = new ArrayList<>();

        for (var itemReq : req.getItems()) {
            Product product = productService.getOrThrow(itemReq.getProductId());
            if (product.getStock() < itemReq.getQuantity())
                throw new InsufficientStockException(product.getName(), itemReq.getQuantity(), product.getStock());

            BigDecimal discountPct = itemReq.getDiscountPercent() != null ? itemReq.getDiscountPercent() : BigDecimal.ZERO;
            BigDecimal baseSubtotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            BigDecimal discountAmount = baseSubtotal.multiply(discountPct)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal itemSubtotal = baseSubtotal.subtract(discountAmount);
            BigDecimal taxRate = product.getTaxRate() != null ? product.getTaxRate() : BigDecimal.ZERO;
            BigDecimal itemTax = itemSubtotal.multiply(taxRate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            SaleItem item = SaleItem.builder()
                    .sale(sale).product(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(product.getPrice())
                    .discountPercent(discountPct)
                    .discountAmount(discountAmount)
                    .subtotal(itemSubtotal)
                    .taxRate(taxRate)
                    .taxAmount(itemTax)
                    .build();

            sale.getItems().add(item);
            totalSubtotal = totalSubtotal.add(itemSubtotal);
            totalTax = totalTax.add(itemTax);

            int before = product.getStock();
            product.setStock(before - itemReq.getQuantity());
            movements.add(StockMovement.builder()
                    .product(product).type(StockMovement.MovementType.EXIT)
                    .quantity(itemReq.getQuantity()).stockBefore(before)
                    .stockAfter(product.getStock()).date(LocalDateTime.now())
                    .reason("Venta " + invoiceNumber).saleId(sale.getId()).build());
        }

        sale.setSubtotal(totalSubtotal);
        sale.setTaxAmount(totalTax);
        sale.setTotal(totalSubtotal.add(totalTax));
        sale = saleRepository.save(sale);
        movementRepository.saveAll(movements);

        if (cashRegister != null) cashRegisterService.addSaleTotal(cashRegister.getId(), sale.getTotal());
        for (var item : sale.getItems()) alertService.checkAndAlert(item.getProduct());

        auditService.log("VENTA_CREADA", "Venta", sale.getId(),
                "Factura: " + sale.getInvoiceNumber() + " | Total: " + sale.getTotal());
        return SaleResponse.from(sale);
    }

    @Transactional(readOnly = true)
    public SaleResponse findById(Long id) {
        return SaleResponse.from(saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada: " + id)));
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> findAll() {
        return saleRepository.findTop10ByOrderByDateDesc().stream()
                .map(SaleResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> findByCashRegister(Long cashRegisterId) {
        return saleRepository.findByCashRegisterIdOrderByDateDesc(cashRegisterId).stream()
                .map(SaleResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> findByDateRange(LocalDateTime from, LocalDateTime to) {
        return saleRepository.findByDateBetweenOrderByDateDesc(from, to).stream()
                .map(SaleResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> searchByInvoice(String invoice) {
        return saleRepository.findByInvoiceNumberContainingIgnoreCaseOrderByDateDesc(invoice).stream()
                .map(SaleResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SaleSummaryResponse getSummary() {
        LocalDateTime startDay = LocalDate.now().atStartOfDay();
        LocalDateTime endDay = startDay.plusDays(1);
        LocalDateTime startMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endMonth = startMonth.plusMonths(1);
        return new SaleSummaryResponse(
                saleRepository.sumTotalBetween(startDay, endDay),
                saleRepository.sumTotalBetween(startMonth, endMonth),
                saleRepository.findByDateBetweenOrderByDateDesc(startDay, endDay).size(),
                saleRepository.findByDateBetweenOrderByDateDesc(startMonth, endMonth).size());
    }
}
