package com.Luis.task_manager.security;

import com.Luis.task_manager.service.AppUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad de la aplicación con Spring Security.
 *
 * <p>Define las reglas de autorización por rol, la página de login personalizada
 * y el proveedor de autenticación basado en la base de datos.</p>
 *
 * <p>Roles disponibles:</p>
 * <ul>
 *   <li>{@code ADMIN} — acceso completo al sistema</li>
 *   <li>{@code SUPERVISOR} — acceso a analíticas, compras y cajas</li>
 *   <li>{@code VENDEDOR} — acceso a ventas y consultas básicas</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AppUserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Define la cadena de filtros de seguridad HTTP.
     *
     * <p>CSRF está deshabilitado porque el frontend consume la API mediante
     * peticiones AJAX con sesión basada en cookies, sin formularios HTML clásicos
     * que requieran protección CSRF adicional.</p>
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF deshabilitado: API consumida por cliente SPA con autenticación por sesión
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // Recursos públicos: login, reset de contraseña y assets estáticos
                .requestMatchers("/login", "/login.html", "/reset-password", "/reset-password.html", "/css/**", "/js/**", "/favicon.ico").permitAll()
                // Endpoints de autenticación pública (reset de contraseña y listado de usuarios para login)
                .requestMatchers("/api/auth/users-public", "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                // Solo ADMIN puede ver el historial de resets de contraseña
                .requestMatchers("/api/auth/password-reset-logs").hasRole("ADMIN")
                // Cualquier usuario autenticado puede gestionar su propio perfil y contraseña
                .requestMatchers(HttpMethod.GET,  "/api/users/profile").authenticated()
                .requestMatchers(HttpMethod.PUT,  "/api/users/profile").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/users/change-password").authenticated()
                // La gestión de usuarios (crear, listar, activar) requiere rol ADMIN
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                // Configuración de empresa solo para ADMIN
                .requestMatchers("/api/empresa/**").hasRole("ADMIN")
                // Reportes y auditoría solo para ADMIN
                .requestMatchers("/api/reports/**").hasRole("ADMIN")
                .requestMatchers("/api/audit/**").hasRole("ADMIN")
                // Analíticas disponibles para ADMIN y SUPERVISOR
                .requestMatchers("/api/analytics/**").hasAnyRole("ADMIN", "SUPERVISOR")
                // Gestión de órdenes de compra para ADMIN y SUPERVISOR
                .requestMatchers("/api/purchases/**").hasAnyRole("ADMIN", "SUPERVISOR")
                // Proveedores solo para ADMIN
                .requestMatchers("/api/suppliers/**").hasRole("ADMIN")
                // Devoluciones: cualquier autenticado puede crear y consultar las suyas
                .requestMatchers(HttpMethod.POST, "/api/returns").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/returns/my").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/returns/by-sale/**").authenticated()
                .requestMatchers("/api/returns/**").hasAnyRole("ADMIN", "SUPERVISOR")
                // Consulta de cajas para todos; operaciones de apertura/cierre requieren rol
                .requestMatchers(HttpMethod.GET, "/api/cajas/**").authenticated()
                .requestMatchers("/api/cajas/**").hasAnyRole("ADMIN", "SUPERVISOR")
                // Cualquier otro endpoint requiere autenticación
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );
        return http.build();
    }

    /**
     * Proveedor de autenticación que valida credenciales contra la base de datos
     * usando BCrypt para verificar las contraseñas.
     */
    @Bean
    public DaoAuthenticationProvider authProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * Expone el {@link AuthenticationManager} como bean para ser inyectado
     * en los controladores que necesiten autenticar programáticamente.
     */
    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
