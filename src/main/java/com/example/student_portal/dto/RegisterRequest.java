package com.example.student_portal.dto;

import com.example.student_portal.entity.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    private String email;
    private String password;
    private UserRole role;
}