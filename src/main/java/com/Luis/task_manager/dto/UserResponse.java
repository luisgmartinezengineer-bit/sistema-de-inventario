package com.Luis.task_manager.dto;

import com.Luis.task_manager.entity.AppUser;
import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String role;
    private boolean active;

    public static UserResponse from(AppUser u) {
        UserResponse r = new UserResponse();
        r.id = u.getId();
        r.username = u.getUsername();
        r.fullName = u.getFullName();
        r.email = u.getEmail();
        r.role = u.getRole().name();
        r.active = u.isActive();
        return r;
    }
}
