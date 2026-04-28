package com.Luis.task_manager.service;

import com.Luis.task_manager.dto.*;
import com.Luis.task_manager.entity.*;
import com.Luis.task_manager.exception.ResourceNotFoundException;
import com.Luis.task_manager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierProductRepository supplierProductRepository;
    private final PriceQuoteRepository priceQuoteRepository;
    private final SupplierEventRepository supplierEventRepository;
    private final SupplierAlertRepository supplierAlertRepository;
    private final ProductRepository productRepository;
    private final PricePredictionService predictionService;
    private final SupplierAlertService alertService;

    // ── Suppliers ─────────────────────────────────────────────────────────

    public List<SupplierResponse> findAll() {
        return supplierRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(s -> {
                    SupplierResponse r = SupplierResponse.from(s);
                    r.setProductCount(supplierProductRepository.findBySupplierIdAndActiveTrue(s.getId()).size());
                    return r;
                }).collect(Collectors.toList());
    }

    public SupplierResponse findById(Long id) {
        Supplier s = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + id));
        SupplierResponse r = SupplierResponse.from(s);
        r.setProductCount(supplierProductRepository.findBySupplierIdAndActiveTrue(id).size());
        return r;
    }

    @Transactional
    public SupplierResponse create(SupplierRequest req) {
        Supplier s = Supplier.builder()
                .name(req.getName()).nit(req.getNit()).contactName(req.getContactName())
                .email(req.getEmail()).phone(req.getPhone()).city(req.getCity())
                .address(req.getAddress()).paymentTermsDays(req.getPaymentTermsDays() != null ? req.getPaymentTermsDays() : 0)
                .leadTimeDays(req.getLeadTimeDays() != null ? req.getLeadTimeDays() : 1)
                .rating(req.getRating() != null ? req.getRating() : BigDecimal.valueOf(3))
                .notes(req.getNotes()).build();
        return SupplierResponse.from(supplierRepository.save(s));
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest req) {
        Supplier s = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + id));
        s.setName(req.getName()); s.setNit(req.getNit()); s.setContactName(req.getContactName());
        s.setEmail(req.getEmail()); s.setPhone(req.getPhone()); s.setCity(req.getCity());
        s.setAddress(req.getAddress());
        if (req.getPaymentTermsDays() != null) s.setPaymentTermsDays(req.getPaymentTermsDays());
        if (req.getLeadTimeDays() != null) s.setLeadTimeDays(req.getLeadTimeDays());
        if (req.getRating() != null) s.setRating(req.getRating());
        s.setNotes(req.getNotes());
        return SupplierResponse.from(supplierRepository.save(s));
    }

    @Transactional
    public void deactivate(Long id) {
        Supplier s = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + id));
        s.setActive(false);
        supplierRepository.save(s);
    }

    // ── Supplier Products ─────────────────────────────────────────────────

    public List<SupplierProductResponse> findProductsBySupplier(Long supplierId) {
        return supplierProductRepository.findBySupplierIdAndActiveTrue(supplierId).stream()
                .map(SupplierProductResponse::from).collect(Collectors.toList());
    }

    public List<SupplierProductResponse> findSuppliersByProduct(Long productId) {
        return supplierProductRepository.findByProductIdAndActiveTrue(productId).stream()
                .map(SupplierProductResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public SupplierProductResponse addSupplierProduct(SupplierProductRequest req) {
        Supplier supplier = supplierRepository.findById(req.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + req.getSupplierId()));
        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + req.getProductId()));

        SupplierProduct sp = SupplierProduct.builder()
                .supplier(supplier).product(product)
                .currentPrice(req.getCurrentPrice())
                .minOrderQuantity(req.getMinOrderQuantity() != null ? req.getMinOrderQuantity() : 1)
                .supplierProductCode(req.getSupplierProductCode())
                .preferred(req.isPreferred())
                .build();
        return SupplierProductResponse.from(supplierProductRepository.save(sp));
    }

    @Transactional
    public void deactivateSupplierProduct(Long spId) {
        SupplierProduct sp = supplierProductRepository.findById(spId)
                .orElseThrow(() -> new ResourceNotFoundException("Relación proveedor-producto no encontrada: " + spId));
        sp.setActive(false);
        supplierProductRepository.save(sp);
    }

    // ── Price Quotes ──────────────────────────────────────────────────────

    public List<PriceQuoteResponse> findQuotes(Long supplierProductId) {
        return priceQuoteRepository.findBySupplierProductIdAndValidTrueOrderByDateDesc(supplierProductId).stream()
                .map(PriceQuoteResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public PriceQuoteResponse addQuote(Long supplierProductId, PriceQuoteRequest req) {
        SupplierProduct sp = supplierProductRepository.findById(supplierProductId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierProduct no encontrado: " + supplierProductId));

        // Calcular variación respecto a la última cotización válida
        List<PriceQuote> prev = priceQuoteRepository.findTop2BySupplierProductIdAndValidTrueOrderByDateDesc(supplierProductId);
        BigDecimal variationPct = null;
        if (!prev.isEmpty()) {
            BigDecimal prevPrice = prev.get(0).getPrice();
            if (prevPrice.compareTo(BigDecimal.ZERO) > 0)
                variationPct = req.getPrice().subtract(prevPrice)
                        .divide(prevPrice, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
        }

        PriceQuote quote = PriceQuote.builder()
                .supplierProduct(sp).price(req.getPrice())
                .date(LocalDateTime.now()).note(req.getNote())
                .variationPercent(variationPct).build();
        quote = priceQuoteRepository.save(quote);

        // Actualizar currentPrice y min/max históricos en SupplierProduct
        sp.setCurrentPrice(req.getPrice());
        sp.setLastQuoteDate(LocalDateTime.now());
        if (sp.getMinHistoricalPrice() == null || req.getPrice().compareTo(sp.getMinHistoricalPrice()) < 0)
            sp.setMinHistoricalPrice(req.getPrice());
        if (sp.getMaxHistoricalPrice() == null || req.getPrice().compareTo(sp.getMaxHistoricalPrice()) > 0)
            sp.setMaxHistoricalPrice(req.getPrice());
        supplierProductRepository.save(sp);

        // Disparar análisis y alertas
        List<PriceQuote> allQuotes = priceQuoteRepository.findBySupplierProductIdAndValidTrueOrderByDateDesc(supplierProductId);
        PriceAnalysisResponse analysis = predictionService.analyze(sp, allQuotes);
        alertService.checkAndAlert(sp, analysis);

        return PriceQuoteResponse.from(quote);
    }

    // ── Price Analysis ────────────────────────────────────────────────────

    public PriceAnalysisResponse analyze(Long supplierProductId) {
        SupplierProduct sp = supplierProductRepository.findById(supplierProductId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierProduct no encontrado: " + supplierProductId));
        List<PriceQuote> quotes = priceQuoteRepository.findBySupplierProductIdAndValidTrueOrderByDateDesc(supplierProductId);

        PriceAnalysisResponse analysis = predictionService.analyze(sp, quotes);

        // Calcular score con contexto de todos los proveedores del mismo producto
        List<SupplierProduct> competitors = supplierProductRepository.findByProductIdAndActiveTrue(sp.getProduct().getId());
        BigDecimal bestPrice = competitors.stream()
                .map(SupplierProduct::getCurrentPrice).filter(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0)
                .min(Comparator.naturalOrder()).orElse(null);
        int bestLeadTime = competitors.stream().mapToInt(c -> c.getSupplier().getLeadTimeDays())
                .filter(d -> d > 0).min().orElse(1);
        int maxPaymentDays = competitors.stream().mapToInt(c -> c.getSupplier().getPaymentTermsDays()).max().orElse(1);

        double totalScore = predictionService.calculateScore(sp, quotes, bestPrice, bestLeadTime, maxPaymentDays);
        analysis.setScore(Math.round(totalScore * 10.0) / 10.0);

        return analysis;
    }

    // ── Supplier Comparison ───────────────────────────────────────────────

    public List<PriceAnalysisResponse> compareSuppliers(Long productId) {
        List<SupplierProduct> suppliers = supplierProductRepository.findByProductIdAndActiveTrue(productId);

        BigDecimal bestPrice = suppliers.stream()
                .map(SupplierProduct::getCurrentPrice).filter(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0)
                .min(Comparator.naturalOrder()).orElse(null);
        int bestLeadTime = suppliers.stream().mapToInt(c -> c.getSupplier().getLeadTimeDays())
                .filter(d -> d > 0).min().orElse(1);
        int maxPaymentDays = suppliers.stream().mapToInt(c -> c.getSupplier().getPaymentTermsDays()).max().orElse(1);

        return suppliers.stream().map(sp -> {
            List<PriceQuote> quotes = priceQuoteRepository.findBySupplierProductIdAndValidTrueOrderByDateDesc(sp.getId());
            PriceAnalysisResponse a = predictionService.analyze(sp, quotes);
            double score = predictionService.calculateScore(sp, quotes, bestPrice, bestLeadTime, maxPaymentDays);
            a.setScore(Math.round(score * 10.0) / 10.0);
            return a;
        }).sorted(Comparator.comparingDouble(PriceAnalysisResponse::getScore).reversed())
                .collect(Collectors.toList());
    }

    // ── Events ────────────────────────────────────────────────────────────

    public List<SupplierEventResponse> findEvents() {
        return supplierEventRepository.findAllByOrderByStartDateDesc().stream()
                .map(SupplierEventResponse::from).collect(Collectors.toList());
    }

    public List<SupplierEventResponse> findUpcomingEvents() {
        LocalDate today = LocalDate.now();
        return supplierEventRepository.findActiveOrUpcoming(today, today.plusDays(60)).stream()
                .map(SupplierEventResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public SupplierEventResponse createEvent(SupplierEventRequest req) {
        SupplierEvent.SupplierEventBuilder builder = SupplierEvent.builder()
                .type(req.getType()).description(req.getDescription())
                .startDate(req.getStartDate()).endDate(req.getEndDate())
                .expectedImpact(req.getExpectedImpact()).intensity(req.getIntensity());
        if (req.getSupplierProductId() != null) {
            SupplierProduct sp = supplierProductRepository.findById(req.getSupplierProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("SupplierProduct no encontrado: " + req.getSupplierProductId()));
            builder.supplierProduct(sp);
        }
        return SupplierEventResponse.from(supplierEventRepository.save(builder.build()));
    }

    // ── Alerts ────────────────────────────────────────────────────────────

    public List<SupplierAlertResponse> findActiveAlerts() {
        return supplierAlertRepository.findByResolvedFalseOrderByCreatedAtDesc().stream()
                .map(SupplierAlertResponse::from).collect(Collectors.toList());
    }

    public List<SupplierAlertResponse> findAllAlerts() {
        return supplierAlertRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(SupplierAlertResponse::from).collect(Collectors.toList());
    }

    // ── Dashboard ─────────────────────────────────────────────────────────

    public SupplierDashboardResponse getDashboard() {
        List<SupplierAlertResponse> active = findActiveAlerts();
        List<SupplierEventResponse> upcoming = findUpcomingEvents();

        List<SupplierAlertResponse> critical = active.stream()
                .filter(a -> a.getType().equals("SUBIDA_PRECIO") || a.getType().equals("TENDENCIA_ALCISTA"))
                .limit(10).collect(Collectors.toList());
        List<SupplierAlertResponse> opportunities = active.stream()
                .filter(a -> a.getType().equals("MINIMO_HISTORICO"))
                .limit(10).collect(Collectors.toList());
        List<SupplierAlertResponse> predictions = active.stream()
                .filter(a -> a.getType().equals("PREDICCION_SUBIDA"))
                .limit(10).collect(Collectors.toList());

        long buyOpps = active.stream().filter(a -> a.getType().equals("MINIMO_HISTORICO")).count();
        long marginRisk = active.stream().filter(a -> a.getType().equals("MARGEN_EN_RIESGO")).count();

        return SupplierDashboardResponse.builder()
                .totalSuppliers((int) supplierRepository.findByActiveTrueOrderByNameAsc().stream().count())
                .activeAlerts(active.size())
                .buyOpportunities((int) buyOpps)
                .marginAtRisk((int) marginRisk)
                .criticalAlerts(critical)
                .opportunities(opportunities)
                .predictions(predictions)
                .upcomingEvents(upcoming)
                .build();
    }
}
