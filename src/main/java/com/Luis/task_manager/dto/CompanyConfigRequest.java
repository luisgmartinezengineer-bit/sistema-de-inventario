package com.Luis.task_manager.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CompanyConfigRequest {

    @Size(max = 150)
    private String razonSocial;

    @Size(max = 100)
    private String nombreComercial;

    /** NIT colombiano: dígitos, opcionalmente con dígito verificador */
    @Pattern(regexp = "^[0-9]{6,15}(-[0-9])?$", message = "NIT inválido (ej: 900123456-7)")
    @Size(max = 20)
    private String nit;

    @Pattern(regexp = "^[0-9]$", message = "El dígito de verificación debe ser un solo dígito")
    private String digitoVerificacion;

    @Size(max = 200)
    private String address;

    @Size(max = 80)
    private String city;

    @Size(max = 80)
    private String department;

    @Pattern(regexp = "^[+]?[0-9][0-9\\-\\s()]{6,18}$", message = "Teléfono inválido (ej: 3001234567 o +57 300 123 4567)")
    private String phone;

    @Email(message = "El correo electrónico no tiene un formato válido")
    @Size(max = 100)
    private String email;

    /** URL del sitio web — debe iniciar con http:// o https:// si se provee */
    @Pattern(regexp = "^(https?://.+)?$", message = "El sitio web debe iniciar con http:// o https://")
    @Size(max = 150)
    private String website;

    @Size(max = 50)
    private String regime;

    @Size(max = 30)
    private String dianResolutionNumber;

    private LocalDate dianResolutionDate;

    @Min(0)
    private Long dianRangeFrom;

    @Min(0)
    private Long dianRangeTo;

    @Size(max = 10)
    private String invoicePrefix;

    @Size(max = 300)
    private String ticketFooter;

    @Email
    @Size(max = 150)
    private String mailUsername;

    @Size(max = 200)
    private String mailPassword;

    @Size(max = 100)
    private String mailFromName;

    @Size(max = 100)
    private String mailHost;

    @Min(1) @Max(65535)
    private Integer mailPort;
}
