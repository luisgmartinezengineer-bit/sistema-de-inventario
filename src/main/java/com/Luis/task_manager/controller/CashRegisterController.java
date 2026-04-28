package com.Luis.task_manager.controller;

import com.Luis.task_manager.dto.CashRegisterRequest;
import com.Luis.task_manager.dto.CashRegisterResponse;
import com.Luis.task_manager.entity.AppUser;
import com.Luis.task_manager.service.CashRegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cajas")
@RequiredArgsConstructor
public class CashRegisterController {

    private final CashRegisterService cashRegisterService;

    @GetMapping
    public List<CashRegisterResponse> findAll(@AuthenticationPrincipal AppUser user) {
        if (user.getRole() == AppUser.Role.VENDEDOR)
            return cashRegisterService.findOpenBySeller(user.getId());
        return cashRegisterService.findAll();
    }

    @GetMapping("/open")
    public List<CashRegisterResponse> findOpen(@AuthenticationPrincipal AppUser user) {
        if (user.getRole() == AppUser.Role.VENDEDOR)
            return cashRegisterService.findOpenBySeller(user.getId());
        return cashRegisterService.findOpen();
    }

    @GetMapping("/{id}")
    public CashRegisterResponse findById(@PathVariable Long id) { return cashRegisterService.findById(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CashRegisterResponse open(@Valid @RequestBody CashRegisterRequest req) {
        return cashRegisterService.open(req);
    }

    @PatchMapping("/{id}/close")
    public CashRegisterResponse close(@PathVariable Long id,
                                       @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        return cashRegisterService.close(id, notes);
    }
}
