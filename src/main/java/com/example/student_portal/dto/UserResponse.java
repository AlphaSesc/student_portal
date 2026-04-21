package com.example.student_portal.dto;

import com.example.student_portal.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// Response DTO exposing user details for client-side use without sensitive data
public class UserResponse {
    private Long id;
    private String email;
    // Indicates user role for role-based access control on client side
    private UserRole role;
}