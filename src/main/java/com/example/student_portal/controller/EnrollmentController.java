package com.example.student_portal.controller;

import com.example.student_portal.dto.EnrollmentRequest;
import com.example.student_portal.dto.EnrollmentResponse;
import com.example.student_portal.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
// REST controller handling student enrollment operations
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    // Enrolls the authenticated student into a course
    public EnrollmentResponse enroll(@Valid @RequestBody EnrollmentRequest request) {
        return enrollmentService.enroll(request);
    }

    // Retrieves enrollments of the currently authenticated student
    @GetMapping("/me")
    public List<EnrollmentResponse> getMyEnrollments() {
        return enrollmentService.getMyEnrollments();
    }
}