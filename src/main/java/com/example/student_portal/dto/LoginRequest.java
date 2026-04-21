package com.example.student_portal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// Request DTO used for user authentication with input validation
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    // Validates that email is provided and follows proper format
    private String email;

    @NotBlank(message = "Password is required")
    // Ensures password is provided for authentication
    private String password;
}