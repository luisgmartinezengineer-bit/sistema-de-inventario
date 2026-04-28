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

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AppUserService userService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/login.html", "/reset-password", "/reset-password.html", "/css/**", "/js/**", "/favicon.ico").permitAll()
                .requestMatchers("/api/auth/users-public", "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                .requestMatchers("/api/auth/password-reset-logs").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,  "/api/users/profile").authenticated()
                .requestMatchers(HttpMethod.PUT,  "/api/users/profile").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/users/change-password").authenticated()
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                .requestMatchers("/api/empresa/**").hasRole("ADMIN")
                .requestMatchers("/api/reports/**").hasRole("ADMIN")
                .requestMatchers("/api/audit/**").hasRole("ADMIN")
                .requestMatchers("/api/analytics/**").hasAnyRole("ADMIN", "SUPERVISOR")
                .requestMatchers("/api/purchases/**").hasAnyRole("ADMIN", "SUPERVISOR")
                .requestMatchers("/api/suppliers/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/returns").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/returns/my").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/returns/by-sale/**").authenticated()
                .requestMatchers("/api/returns/**").hasAnyRole("ADMIN", "SUPERVISOR")
                .requestMatchers(HttpMethod.GET, "/api/cajas/**").authenticated()
                .requestMatchers("/api/cajas/**").hasAnyRole("ADMIN", "SUPERVISOR")
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

    @Bean
    public DaoAuthenticationProvider authProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
