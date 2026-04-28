package com.Luis.task_manager.security;

import com.Luis.task_manager.entity.AppUser;
import com.Luis.task_manager.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        userRepository.findByUsername("admin").ifPresentOrElse(
            existing -> {
                // Garantizar que siempre sea ADMIN activo con contraseña conocida
                existing.setPassword(passwordEncoder.encode("admin123"));
                existing.setRole(AppUser.Role.ADMIN);
                existing.setActive(true);
                userRepository.save(existing);
                log.info("Contraseña admin restablecida — usuario: admin");
            },
            () -> {
                userRepository.save(AppUser.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .fullName("Administrador")
                        .role(AppUser.Role.ADMIN)
                        .active(true)
                        .build());
                log.info("Usuario admin creado — usuario: admin");
            }
        );
    }
}
