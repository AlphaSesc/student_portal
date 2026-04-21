package com.example.student_portal.controller;

import com.example.student_portal.dto.GraduationEligibilityResponse;
import com.example.student_portal.dto.StudentProfileResponse;
import com.example.student_portal.dto.UpdateStudentProfileRequest;
import com.example.student_portal.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
// REST controller for student profile management and eligibility-related operations
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/me")
    // Retrieves profile of the authenticated student
    public StudentProfileResponse getMyProfile() {
        return studentService.getMyProfile();
    }

    @PutMapping("/me")
    // Updates profile details of the authenticated student
    public StudentProfileResponse updateMyProfile(@Valid @RequestBody UpdateStudentProfileRequest request) {
        return studentService.updateMyProfile(request);
    }

    @GetMapping("/me/graduation-eligibility")
    // Checks whether the student is eligible for graduation based on financial status
    public GraduationEligibilityResponse checkGraduationEligibility() {
        return studentService.checkGraduationEligibility();
    }
}