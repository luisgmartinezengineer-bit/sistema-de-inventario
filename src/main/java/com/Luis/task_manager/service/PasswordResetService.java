package com.Luis.task_manager.service;

import com.Luis.task_manager.dto.PasswordResetLogResponse;
import com.Luis.task_manager.entity.AppUser;
import com.Luis.task_manager.entity.CompanyConfig;
import com.Luis.task_manager.entity.PasswordResetLog;
import com.Luis.task_manager.entity.PasswordResetToken;
import com.Luis.task_manager.repository.AppUserRepository;
import com.Luis.task_manager.repository.PasswordResetLogRepository;
import com.Luis.task_manager.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PasswordResetService {

    @Value("${app.base-url}")
    private String baseUrl;

    private final AppUserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetLogRepository logRepository;
    private final CompanyConfigService configService;
    private final PasswordEncoder passwordEncoder;

    /** Solicita el reset validando que usuario y correo coincidan. Devuelve el email enmascarado. */
    public String requestReset(String username, String email, String ipAddress) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("El usuario o correo no coinciden con ninguna cuenta registrada."));

        if (user.getEmail() == null || user.getEmail().isBlank())
            throw new RuntimeException("Este usuario no tiene un correo registrado. Contacta al administrador.");

        if (!user.getEmail().equalsIgnoreCase(email.trim()))
            throw new RuntimeException("El usuario o correo no coinciden con ninguna cuenta registrada.");

        CompanyConfig config = configService.getOrCreate();
        if (config.getMailUsername() == null || config.getMailPassword() == null)
            throw new RuntimeException("El correo SMTP no está configurado. Contacta al administrador.");

        tokenRepository.deleteByUsername(username);
        tokenRepository.deleteExpired(LocalDateTime.now());

        String token = UUID.randomUUID().toString();
        tokenRepository.save(PasswordResetToken.builder()
                .token(token)
                .username(username)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build());

        String masked = maskEmail(user.getEmail());

        logRepository.save(PasswordResetLog.builder()
                .username(username)
                .maskedEmail(masked)
                .ipAddress(ipAddress)
                .action(PasswordResetLog.Action.REQUEST)
                .createdAt(LocalDateTime.now())
                .build());

        sendResetEmail(config, user.getEmail(), user.getFullName(), token);

        return masked;
    }

    public void resetPassword(String token, String newPassword, String ipAddress) {
        PasswordResetToken prt = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token inválido o expirado"));

        if (prt.isUsed()) throw new RuntimeException("El token ya fue utilizado");
        if (prt.getExpiresAt().isBefore(LocalDateTime.now())) throw new RuntimeException("El token ha expirado");

        AppUser user = userRepository.findByUsername(prt.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        prt.setUsed(true);
        tokenRepository.save(prt);

        String masked = user.getEmail() != null ? maskEmail(user.getEmail()) : "—";
        logRepository.save(PasswordResetLog.builder()
                .username(prt.getUsername())
                .maskedEmail(masked)
                .ipAddress(ipAddress)
                .action(PasswordResetLog.Action.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional(readOnly = true)
    public List<PasswordResetLogResponse> getLogs() {
        return logRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(PasswordResetLogResponse::from)
                .toList();
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 0) return "***";
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 3) return local.charAt(0) + "***" + domain;
        if (local.length() <= 6) return local.substring(0, 3) + "***" + domain;
        return local.substring(0, 3) + "***" + local.substring(local.length() - 3) + domain;
    }

    private void sendResetEmail(CompanyConfig config, String toEmail, String fullName, String token) {
        try {
            JavaMailSenderImpl sender = buildSender(config);
            String resetUrl = baseUrl + "/reset-password?token=" + token;
            String fromName = config.getMailFromName() != null ? config.getMailFromName() : "Inventario Pro";

            var message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(config.getMailUsername(), fromName);
            helper.setTo(toEmail);
            helper.setSubject("Recuperación de contraseña — " + fromName);
            helper.setText(buildEmailHtml(fullName, resetUrl, fromName), true);
            sender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Error al enviar el correo: " + e.getMessage());
        }
    }

    private JavaMailSenderImpl buildSender(CompanyConfig config) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getMailHost() != null ? config.getMailHost() : "smtp.gmail.com");
        sender.setPort(config.getMailPort() != null ? config.getMailPort() : 587);
        sender.setUsername(config.getMailUsername());
        sender.setPassword(config.getMailPassword());
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        return sender;
    }

    private String buildEmailHtml(String fullName, String resetUrl, String appName) {
        return """
            <div style="font-family:Segoe UI,Arial,sans-serif;max-width:560px;margin:auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 8px 32px rgba(0,0,0,.12)">
              <div style="background:linear-gradient(135deg,#1a2540,#2d3f6e);padding:36px 32px;text-align:center">
                <div style="width:64px;height:64px;background:rgba(255,255,255,.15);border-radius:50%%;display:inline-flex;align-items:center;justify-content:center;margin-bottom:16px">
                  <span style="font-size:2rem">🔑</span>
                </div>
                <h2 style="color:#fff;margin:0;font-size:1.5rem;font-weight:700">Recuperar contraseña</h2>
                <p style="color:#a9c0f0;margin:8px 0 0;font-size:.9rem">%s</p>
              </div>
              <div style="padding:36px 32px">
                <p style="color:#1a2540;font-size:1rem;font-weight:600;margin:0 0 8px">Hola, <span style="color:#0d6efd">%s</span></p>
                <p style="color:#555;font-size:.92rem;line-height:1.6;margin:0 0 28px">
                  Recibimos una solicitud para restablecer la contraseña de tu cuenta.<br>
                  Haz clic en el botón a continuación — el enlace es válido por <strong>1 hora</strong>.
                </p>
                <div style="text-align:center;margin:0 0 28px">
                  <a href="%s" style="display:inline-block;background:linear-gradient(135deg,#0d6efd,#0a58ca);color:#fff;padding:16px 40px;border-radius:12px;text-decoration:none;font-weight:700;font-size:1rem;letter-spacing:.3px;box-shadow:0 4px 16px rgba(13,110,253,.35)">
                    Restablecer contraseña
                  </a>
                </div>
                <div style="background:#f8f9fa;border-radius:10px;padding:14px 18px;margin-bottom:20px">
                  <p style="color:#888;font-size:.82rem;margin:0;line-height:1.5">
                    <strong style="color:#555">¿No solicitaste esto?</strong><br>
                    Ignora este correo de forma segura. Tu contraseña no cambiará hasta que hagas clic en el enlace de arriba.
                  </p>
                </div>
                <hr style="border:none;border-top:1px solid #eee;margin:20px 0">
                <p style="color:#bbb;font-size:.78rem;text-align:center;margin:0">%s — Sistema de Inventario</p>
              </div>
            </div>
            """.formatted(appName, fullName, resetUrl, appName);
    }
}
