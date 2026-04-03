package com.example.student_portal.dto;

import com.example.student_portal.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String email;
    private UserRole role;
}