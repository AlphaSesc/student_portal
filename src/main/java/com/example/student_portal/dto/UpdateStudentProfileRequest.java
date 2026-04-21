package com.example.student_portal.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Request DTO for updating student profile with validation constraints on input fields
public class UpdateStudentProfileRequest {

    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    // Ensures valid name length while allowing partial updates
    private String firstName;

    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @Pattern(regexp = "^[0-9]{7,15}$", message = "Phone must be valid")
    // Validates phone number format to prevent invalid contact data
    private String phone;

    @Size(max = 500, message = "Address too long")
    private String address;
}