package com.Luis.task_manager.service;

import com.Luis.task_manager.dto.CashRegisterRequest;
import com.Luis.task_manager.dto.CashRegisterResponse;
import com.Luis.task_manager.entity.AppUser;
import com.Luis.task_manager.entity.CashRegister;
import com.Luis.task_manager.exception.ResourceNotFoundException;
import com.Luis.task_manager.repository.AppUserRepository;
import com.Luis.task_manager.repository.CashRegisterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CashRegisterService {

    private final CashRegisterRepository cashRegisterRepository;
    private final AppUserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CashRegisterResponse> findAll() {
        return cashRegisterRepository.findAllByOrderByOpenedAtDesc().stream()
                .map(CashRegisterResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CashRegisterResponse> findOpen() {
        return cashRegisterRepository.findByStatusOrderByOpenedAtDesc(CashRegister.CashStatus.OPEN).stream()
                .map(CashRegisterResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CashRegisterResponse> findOpenBySeller(Long sellerId) {
        return cashRegisterRepository.findAllBySellerIdAndStatus(sellerId, CashRegister.CashStatus.OPEN).stream()
                .map(CashRegisterResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CashRegisterResponse findById(Long id) {
        return CashRegisterResponse.from(getOrThrow(id));
    }

    public CashRegisterResponse open(CashRegisterRequest req) {
        AppUser seller = req.getSellerId() != null
                ? userRepository.findById(req.getSellerId()).orElse(null)
                : null;

        if (seller != null) {
            cashRegisterRepository.findBySellerIdAndStatus(seller.getId(), CashRegister.CashStatus.OPEN)
                    .ifPresent(c -> { throw new IllegalArgumentException("El vendedor ya tiene una caja abierta: " + c.getName()); });
        }

        CashRegister caja = CashRegister.builder()
                .name(req.getName())
                .seller(seller)
                .status(CashRegister.CashStatus.OPEN)
                .openedAt(LocalDateTime.now())
                .initialAmount(req.getInitialAmount())
                .totalSales(BigDecimal.ZERO)
                .totalExpenses(BigDecimal.ZERO)
                .notes(req.getNotes())
                .build();
        return CashRegisterResponse.from(cashRegisterRepository.save(caja));
    }

    public CashRegisterResponse close(Long id, String notes) {
        CashRegister caja = getOrThrow(id);
        if (caja.getStatus() == CashRegister.CashStatus.CLOSED)
            throw new IllegalArgumentException("La caja ya está cerrada");
        caja.setStatus(CashRegister.CashStatus.CLOSED);
        caja.setClosedAt(LocalDateTime.now());
        if (notes != null && !notes.isBlank()) caja.setNotes(notes);
        return CashRegisterResponse.from(cashRegisterRepository.save(caja));
    }

    public void addSaleTotal(Long cajaId, BigDecimal amount) {
        CashRegister caja = getOrThrow(cajaId);
        caja.setTotalSales(caja.getTotalSales().add(amount));
        cashRegisterRepository.save(caja);
    }

    public CashRegister getOrThrow(Long id) {
        return cashRegisterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caja no encontrada: " + id));
    }
}
