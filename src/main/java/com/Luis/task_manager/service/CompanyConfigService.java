package com.Luis.task_manager.service;

import com.Luis.task_manager.dto.CompanyConfigRequest;
import com.Luis.task_manager.dto.CompanyConfigResponse;
import com.Luis.task_manager.entity.CompanyConfig;
import com.Luis.task_manager.repository.CompanyConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CompanyConfigService {

    private final CompanyConfigRepository configRepository;

    @Transactional(readOnly = true)
    public CompanyConfigResponse get() {
        return CompanyConfigResponse.from(getOrCreate());
    }

    public CompanyConfigResponse save(CompanyConfigRequest req) {
        CompanyConfig config = getOrCreate();
        config.setRazonSocial(req.getRazonSocial());
        config.setNombreComercial(req.getNombreComercial());
        config.setNit(req.getNit());
        config.setDigitoVerificacion(req.getDigitoVerificacion());
        config.setAddress(req.getAddress());
        config.setCity(req.getCity());
        config.setDepartment(req.getDepartment());
        config.setPhone(req.getPhone());
        config.setEmail(req.getEmail());
        config.setWebsite(req.getWebsite());
        config.setDianResolutionNumber(req.getDianResolutionNumber());
        config.setDianResolutionDate(req.getDianResolutionDate());
        config.setDianRangeFrom(req.getDianRangeFrom());
        config.setDianRangeTo(req.getDianRangeTo());
        if (req.getInvoicePrefix() != null) config.setInvoicePrefix(req.getInvoicePrefix());
        if (req.getTicketFooter() != null) config.setTicketFooter(req.getTicketFooter());
        if (req.getMailUsername() != null) config.setMailUsername(req.getMailUsername().isBlank() ? null : req.getMailUsername().trim());
        if (req.getMailPassword() != null) config.setMailPassword(req.getMailPassword().isBlank() ? null : req.getMailPassword());
        if (req.getMailFromName() != null) config.setMailFromName(req.getMailFromName().isBlank() ? null : req.getMailFromName().trim());
        if (req.getMailHost() != null) config.setMailHost(req.getMailHost().isBlank() ? "smtp.gmail.com" : req.getMailHost().trim());
        if (req.getMailPort() != null) config.setMailPort(req.getMailPort());
        if (req.getRegime() != null) {
            try { config.setRegime(CompanyConfig.Regime.valueOf(req.getRegime())); } catch (IllegalArgumentException e) {
                log.warn("Régimen fiscal desconocido '{}', se conserva el valor actual", req.getRegime());
            }
        }
        return CompanyConfigResponse.from(configRepository.save(config));
    }

    public synchronized String generateNextInvoiceNumber() {
        CompanyConfig config = getOrCreate();
        long next = config.getCurrentInvoiceNumber() + 1;
        config.setCurrentInvoiceNumber(next);
        configRepository.save(config);
        return config.getInvoicePrefix() + String.format("%08d", next);
    }

    public CompanyConfig getOrCreate() {
        return configRepository.findAll().stream().findFirst()
                .orElseGet(() -> configRepository.save(CompanyConfig.builder()
                        .razonSocial("Mi Empresa S.A.S.")
                        .nit("900000000")
                        .digitoVerificacion("0")
                        .city("Bogotá")
                        .department("Cundinamarca")
                        .regime(CompanyConfig.Regime.RESPONSABLE_IVA)
                        .invoicePrefix("FV")
                        .currentInvoiceNumber(0L)
                        .ticketFooter("¡Gracias por su compra!")
                        .build()));
    }
}
