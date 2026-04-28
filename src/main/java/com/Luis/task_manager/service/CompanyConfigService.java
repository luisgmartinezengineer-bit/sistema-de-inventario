package com.Luis.task_manager.service;

import com.Luis.task_manager.dto.CompanyConfigRequest;
import com.Luis.task_manager.dto.CompanyConfigResponse;
import com.Luis.task_manager.entity.CompanyConfig;
import com.Luis.task_manager.repository.CompanyConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para gestionar la configuración de la empresa.
 *
 * <p>La configuración incluye datos fiscales (NIT, régimen, resolución DIAN),
 * información de contacto y parámetros de facturación como el prefijo y el
 * contador de número de factura.</p>
 *
 * <p>El sistema garantiza que siempre exista exactamente un registro de configuración,
 * creándolo con valores por defecto si no existe (patrón "get or create").</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CompanyConfigService {

    private final CompanyConfigRepository configRepository;

    /**
     * Retorna la configuración actual de la empresa.
     * Si no existe ningún registro, crea uno con valores por defecto.
     */
    @Transactional(readOnly = true)
    public CompanyConfigResponse get() {
        return CompanyConfigResponse.from(getOrCreate());
    }

    /**
     * Actualiza la configuración de la empresa con los valores proporcionados.
     * Los campos de correo electrónico se limpian de espacios antes de guardarse.
     *
     * @param req nuevos valores de configuración
     * @return la configuración actualizada
     */
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
            // Si el valor enviado no corresponde a ningún régimen válido, se ignora el cambio
            try { config.setRegime(CompanyConfig.Regime.valueOf(req.getRegime())); } catch (IllegalArgumentException e) {
                log.warn("Régimen fiscal desconocido '{}', se conserva el valor actual", req.getRegime());
            }
        }
        return CompanyConfigResponse.from(configRepository.save(config));
    }

    /**
     * Genera el siguiente número de factura de forma sincronizada para evitar duplicados
     * en entornos con múltiples hilos concurrentes.
     *
     * <p>El formato del número es: {@code [prefijo] + número de 8 dígitos con ceros a la izquierda}
     * (ej. {@code FV00000001}).</p>
     *
     * @return número de factura generado
     */
    public synchronized String generateNextInvoiceNumber() {
        CompanyConfig config = getOrCreate();
        long next = config.getCurrentInvoiceNumber() + 1;
        config.setCurrentInvoiceNumber(next);
        configRepository.save(config);
        return config.getInvoicePrefix() + String.format("%08d", next);
    }

    /**
     * Retorna la configuración de empresa existente o crea una nueva con valores por defecto
     * si no hay ningún registro en la base de datos.
     */
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
