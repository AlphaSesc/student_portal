package com.example.student_portal.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Response DTO indicating whether a student meets graduation requirements
public class GraduationEligibilityResponse {
    private String studentId;
    private boolean eligible;
    private String message;
}