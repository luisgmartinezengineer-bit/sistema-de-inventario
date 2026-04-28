package com.Luis.task_manager.security;

import com.Luis.task_manager.entity.AppUser;
import com.Luis.task_manager.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Inicializador de datos que se ejecuta al arrancar la aplicación.
 *
 * <p>Garantiza que siempre exista un usuario administrador activo en el sistema.
 * Si el usuario {@code admin} ya existe, se restablece su contraseña y rol para
 * asegurar el acceso inicial. Si no existe, se crea con los valores por defecto.</p>
 *
 * <p>Este comportamiento es intencional para entornos de desarrollo y demo,
 * donde se necesita un acceso garantizado al sistema en todo momento.</p>
 */
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
                // Garantiza que el admin siempre tenga el rol correcto y esté activo
                existing.setPassword(passwordEncoder.encode("admin123"));
                existing.setRole(AppUser.Role.ADMIN);
                existing.setActive(true);
                userRepository.save(existing);
                log.info("Contraseña admin restablecida — usuario: admin");
            },
            () -> {
                // Crea el usuario administrador si no existe en la base de datos
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
