package com.Luis.task_manager.service;

import com.Luis.task_manager.dto.UserProfileRequest;
import com.Luis.task_manager.dto.UserProfileResponse;
import com.Luis.task_manager.dto.UserRequest;
import com.Luis.task_manager.dto.UserResponse;
import com.Luis.task_manager.entity.AppUser;
import com.Luis.task_manager.exception.ResourceNotFoundException;
import com.Luis.task_manager.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AppUserService implements UserDetailsService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(UserResponse::from).collect(Collectors.toList());
    }

    public UserResponse create(UserRequest req) {
        if (userRepository.existsByUsername(req.getUsername()))
            throw new IllegalArgumentException("El usuario ya existe: " + req.getUsername());
        if (req.getPassword() == null || req.getPassword().isBlank())
            throw new IllegalArgumentException("La contraseña es obligatoria");

        AppUser user = AppUser.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .email(req.getEmail())
                .role(AppUser.Role.valueOf(req.getRole().toUpperCase()))
                .active(true)
                .build();
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse update(Long id, UserRequest req) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());
        user.setRole(AppUser.Role.valueOf(req.getRole().toUpperCase()));
        if (req.getPassword() != null && !req.getPassword().isBlank())
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        return UserResponse.from(userRepository.save(user));
    }

    public void toggleActive(Long id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
        user.setActive(!user.isActive());
        userRepository.save(user);
    }

    public void changePassword(String username, String currentPassword, String newPassword) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (!passwordEncoder.matches(currentPassword, user.getPassword()))
            throw new RuntimeException("La contraseña actual es incorrecta");
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return UserProfileResponse.from(user);
    }

    public UserProfileResponse updateProfile(String username, UserProfileRequest req) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword()))
            throw new RuntimeException("La contraseña es incorrecta. No se guardaron los cambios.");

        if (req.getFullName() != null && !req.getFullName().isBlank()) user.setFullName(req.getFullName().trim());
        if (req.getEmail()    != null) user.setEmail(req.getEmail().isBlank() ? null : req.getEmail().trim());
        if (req.getPhone()    != null) user.setPhone(req.getPhone().isBlank() ? null : req.getPhone().trim());
        user.setBirthDate(req.getBirthDate());
        if (req.getBloodType()     != null) user.setBloodType(req.getBloodType().isBlank() ? null : req.getBloodType());
        if (req.getDocumentType()  != null) user.setDocumentType(req.getDocumentType().isBlank() ? null : req.getDocumentType());
        if (req.getDocumentNumber()!= null) user.setDocumentNumber(req.getDocumentNumber().isBlank() ? null : req.getDocumentNumber().trim());
        if (req.getAddress()       != null) user.setAddress(req.getAddress().isBlank() ? null : req.getAddress().trim());
        if (req.getCity()          != null) user.setCity(req.getCity().isBlank() ? null : req.getCity().trim());
        if (req.getDepartment()    != null) user.setDepartment(req.getDepartment().isBlank() ? null : req.getDepartment().trim());
        if (req.getPosition()      != null) user.setPosition(req.getPosition().isBlank() ? null : req.getPosition().trim());
        if (req.getNotes()         != null) user.setNotes(req.getNotes().isBlank() ? null : req.getNotes().trim());
        if (req.getEmergencyContactName()    != null) user.setEmergencyContactName(req.getEmergencyContactName().isBlank() ? null : req.getEmergencyContactName().trim());
        if (req.getEmergencyContactPhone()   != null) user.setEmergencyContactPhone(req.getEmergencyContactPhone().isBlank() ? null : req.getEmergencyContactPhone().trim());
        if (req.getEmergencyContactRelation()!= null) user.setEmergencyContactRelation(req.getEmergencyContactRelation().isBlank() ? null : req.getEmergencyContactRelation().trim());

        return UserProfileResponse.from(userRepository.save(user));
    }
}
