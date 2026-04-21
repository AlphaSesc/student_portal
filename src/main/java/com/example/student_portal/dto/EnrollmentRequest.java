package com.example.student_portal.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Request DTO used to enroll a student in a course
public class EnrollmentRequest {

    @NotNull(message = "Course id is required")
    // Ensures courseId is provided before processing enrollment
    private Long courseId;
}