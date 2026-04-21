package com.example.student_portal.dto;

import com.example.student_portal.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// Response DTO returned after successful authentication containing JWT token,
// user identity, and role information for secure client-side authorization
public class AuthResponse {
    private String token;
    private String email;
    private UserRole role;
}