package com.Luis.task_manager.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserRequest {

    /** Solo letras, dígitos, puntos, guiones bajos y guiones. Sin espacios. */
    @NotBlank
    @Size(min = 3, max = 30, message = "El usuario debe tener entre 3 y 30 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El usuario solo puede contener letras, dígitos, puntos, guiones y guiones bajos")
    private String username;

    /** Mínimo 8 caracteres. Opcional en actualización (null = no cambiar). */
    @Size(min = 8, max = 100, message = "La contraseña debe tener mínimo 8 caracteres")
    private String password;

    @NotBlank
    @Size(max = 100)
    private String fullName;

    @NotBlank
    @Pattern(regexp = "^(ADMIN|SUPERVISOR|VENDEDOR)$", message = "El rol debe ser ADMIN, SUPERVISOR o VENDEDOR")
    private String role;

    @Email(message = "Correo inválido")
    @Size(max = 150)
    private String email;
}
