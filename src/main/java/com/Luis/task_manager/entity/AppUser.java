package com.Luis.task_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "app_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder.Default
    private boolean active = true;

    // ── Perfil personal ──────────────────────────────
    private String phone;
    private LocalDate birthDate;
    private String bloodType;          // A+, A-, B+, B-, O+, O-, AB+, AB-
    private String documentType;       // CC, CE, TI, Pasaporte, NIT
    private String documentNumber;
    private String address;
    private String city;
    private String department;
    private String position;           // Cargo / puesto
    private String notes;              // Notas internas

    // ── Contacto de emergencia ───────────────────────
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation; // Familiar, Amigo, Colega...

    public enum Role { ADMIN, SUPERVISOR, VENDEDOR }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return active; }
}
