package com.Luis.task_manager.dto;

import com.Luis.task_manager.entity.CompanyConfig;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CompanyConfigResponse {
    private Long id;
    private String razonSocial;
    private String nombreComercial;
    private String nit;
    private String digitoVerificacion;
    private String nitFormatted;      // 900.123.456-7
    private String address;
    private String city;
    private String department;
    private String phone;
    private String email;
    private String website;
    private String regime;
    private String regimeLabel;
    private String dianResolutionNumber;
    private LocalDate dianResolutionDate;
    private Long dianRangeFrom;
    private Long dianRangeTo;
    private String invoicePrefix;
    private Long currentInvoiceNumber;
    private String ticketFooter;
    private String mailUsername;
    private String mailFromName;
    private String mailHost;
    private Integer mailPort;
    private boolean mailConfigured;

    public static CompanyConfigResponse from(CompanyConfig c) {
        CompanyConfigResponse r = new CompanyConfigResponse();
        r.id = c.getId();
        r.razonSocial = c.getRazonSocial();
        r.nombreComercial = c.getNombreComercial();
        r.nit = c.getNit();
        r.digitoVerificacion = c.getDigitoVerificacion();
        r.nitFormatted = formatNit(c.getNit(), c.getDigitoVerificacion());
        r.address = c.getAddress();
        r.city = c.getCity();
        r.department = c.getDepartment();
        r.phone = c.getPhone();
        r.email = c.getEmail();
        r.website = c.getWebsite();
        r.regime = c.getRegime() != null ? c.getRegime().name() : null;
        r.regimeLabel = regimeLabel(c.getRegime());
        r.dianResolutionNumber = c.getDianResolutionNumber();
        r.dianResolutionDate = c.getDianResolutionDate();
        r.dianRangeFrom = c.getDianRangeFrom();
        r.dianRangeTo = c.getDianRangeTo();
        r.invoicePrefix = c.getInvoicePrefix();
        r.currentInvoiceNumber = c.getCurrentInvoiceNumber();
        r.ticketFooter = c.getTicketFooter();
        r.mailUsername = c.getMailUsername();
        r.mailFromName = c.getMailFromName();
        r.mailHost = c.getMailHost();
        r.mailPort = c.getMailPort();
        r.mailConfigured = c.getMailUsername() != null && c.getMailPassword() != null;
        return r;
    }

    private static String formatNit(String nit, String dv) {
        if (nit == null) return "";
        String clean = nit.replaceAll("[^0-9]", "");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < clean.length(); i++) {
            if (i > 0 && (clean.length() - i) % 3 == 0) sb.append('.');
            sb.append(clean.charAt(i));
        }
        return dv != null ? "NIT: " + sb + "-" + dv : "NIT: " + sb;
    }

    private static String regimeLabel(CompanyConfig.Regime r) {
        if (r == null) return "";
        return switch (r) {
            case RESPONSABLE_IVA -> "Responsable del IVA";
            case NO_RESPONSABLE_IVA -> "No Responsable del IVA";
            case GRAN_CONTRIBUYENTE -> "Gran Contribuyente";
            case REGIMEN_SIMPLE -> "Régimen Simple de Tributación";
        };
    }
}
