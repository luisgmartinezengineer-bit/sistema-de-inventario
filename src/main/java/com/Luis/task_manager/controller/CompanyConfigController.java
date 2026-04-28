package com.Luis.task_manager.controller;

import com.Luis.task_manager.dto.CompanyConfigRequest;
import com.Luis.task_manager.dto.CompanyConfigResponse;
import com.Luis.task_manager.service.CompanyConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/empresa")
@RequiredArgsConstructor
public class CompanyConfigController {

    private final CompanyConfigService configService;

    @GetMapping
    public CompanyConfigResponse get() { return configService.get(); }

    @PostMapping
    public CompanyConfigResponse save(@Valid @RequestBody CompanyConfigRequest req) {
        return configService.save(req);
    }
}
