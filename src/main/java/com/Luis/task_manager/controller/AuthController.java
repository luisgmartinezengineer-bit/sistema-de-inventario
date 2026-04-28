package com.Luis.task_manager.controller;

import com.Luis.task_manager.dto.UserResponse;
import com.Luis.task_manager.entity.AppUser;
import com.Luis.task_manager.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AppUserRepository userRepository;

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AppUser user) {
        return UserResponse.from(user);
    }

    // Endpoint público: solo devuelve nombre visible y rol — sin datos sensibles
    @GetMapping("/users-public")
    public List<UserResponse> usersPublic() {
        return userRepository.findAll().stream()
                .filter(AppUser::isActive)
                .map(UserResponse::from)
                .toList();
    }
}
