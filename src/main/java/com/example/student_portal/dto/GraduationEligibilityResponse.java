package com.example.student_portal.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraduationEligibilityResponse {
    private String studentId;
    private boolean eligible;
    private String message;
}