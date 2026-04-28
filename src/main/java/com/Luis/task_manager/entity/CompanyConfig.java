package com.Luis.task_manager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "company_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String razonSocial;
    private String nombreComercial;

    private String nit;                  // sin dígito de verificación
    private String digitoVerificacion;   // dígito de verificación del NIT

    private String address;
    private String city;
    private String department;
    private String phone;
    private String email;
    private String website;

    // DIAN
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Regime regime = Regime.RESPONSABLE_IVA;

    private String dianResolutionNumber;
    private LocalDate dianResolutionDate;
    private Long dianRangeFrom;
    private Long dianRangeTo;

    @Builder.Default
    private String invoicePrefix = "FV";

    @Builder.Default
    private Long currentInvoiceNumber = 0L;

    @Builder.Default
    private String ticketFooter = "¡Gracias por su compra!";

    // Configuración SMTP para recuperación de contraseña
    private String mailUsername;
    private String mailPassword;
    private String mailFromName;

    @Builder.Default
    private String mailHost = "smtp.gmail.com";

    @Builder.Default
    private Integer mailPort = 587;

    public enum Regime {
        RESPONSABLE_IVA,
        NO_RESPONSABLE_IVA,
        GRAN_CONTRIBUYENTE,
        REGIMEN_SIMPLE
    }
}
