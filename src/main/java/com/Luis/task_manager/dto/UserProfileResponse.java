package com.Luis.task_manager.dto;

import com.Luis.task_manager.entity.AppUser;
import lombok.Data;

import java.time.LocalDate;
import java.time.Period;

@Data
public class UserProfileResponse {

    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String role;
    private String roleLabel;

    private String phone;
    private LocalDate birthDate;
    private Integer age;
    private String bloodType;
    private String documentType;
    private String documentNumber;
    private String address;
    private String city;
    private String department;
    private String position;
    private String notes;

    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;

    public static UserProfileResponse from(AppUser u) {
        UserProfileResponse r = new UserProfileResponse();
        r.id = u.getId();
        r.username = u.getUsername();
        r.fullName = u.getFullName();
        r.email = u.getEmail();
        r.role = u.getRole() != null ? u.getRole().name() : null;
        r.roleLabel = roleLabel(u.getRole());
        r.phone = u.getPhone();
        r.birthDate = u.getBirthDate();
        r.age = u.getBirthDate() != null ? Period.between(u.getBirthDate(), LocalDate.now()).getYears() : null;
        r.bloodType = u.getBloodType();
        r.documentType = u.getDocumentType();
        r.documentNumber = u.getDocumentNumber();
        r.address = u.getAddress();
        r.city = u.getCity();
        r.department = u.getDepartment();
        r.position = u.getPosition();
        r.notes = u.getNotes();
        r.emergencyContactName = u.getEmergencyContactName();
        r.emergencyContactPhone = u.getEmergencyContactPhone();
        r.emergencyContactRelation = u.getEmergencyContactRelation();
        return r;
    }

    private static String roleLabel(AppUser.Role role) {
        if (role == null) return "";
        return switch (role) {
            case ADMIN -> "Administrador";
            case SUPERVISOR -> "Supervisor";
            case VENDEDOR -> "Vendedor";
        };
    }
}
