package com.Luis.task_manager.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CustomerRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    /** Cédula, NIT u otro documento: solo dígitos y guion de verificación */
    @Pattern(regexp = "^[0-9]{5,20}(-[0-9])?$", message = "Formato de documento inválido (solo dígitos, ej: 1234567890)")
    @Size(max = 25)
    private String document;

    @Email(message = "El correo electrónico no tiene un formato válido")
    @Size(max = 100)
    private String email;

    /** Acepta dígitos, +, -, espacios y paréntesis; mínimo 7 caracteres */
    @Pattern(regexp = "^[+]?[0-9][0-9\\-\\s()]{6,18}$", message = "Teléfono inválido (ej: 3001234567 o +57 300 123 4567)")
    private String phone;

    @Size(max = 200)
    private String address;
}
