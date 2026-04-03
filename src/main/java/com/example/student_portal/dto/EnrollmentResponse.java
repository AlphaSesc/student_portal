package com.example.student_portal.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentResponse {

    private Long enrollmentId;
    private String studentId;
    private Long courseId;
    private String courseCode;
    private String courseTitle;
    private LocalDateTime enrolledAt;
}