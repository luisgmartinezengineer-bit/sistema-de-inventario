package com.Luis.task_manager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserProfileRequest {

    // Datos básicos
    private String fullName;
    private String email;
    private String phone;
    private LocalDate birthDate;
    private String bloodType;
    private String documentType;
    private String documentNumber;
    private String address;
    private String city;
    private String department;
    private String position;
    private String notes;

    // Contacto de emergencia
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;

    // Contraseña requerida para guardar
    @NotBlank
    private String currentPassword;
}
