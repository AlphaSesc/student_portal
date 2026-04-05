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
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/me")
    public StudentProfileResponse getMyProfile() {
        return studentService.getMyProfile();
    }

    @PutMapping("/me")
    public StudentProfileResponse updateMyProfile(@Valid @RequestBody UpdateStudentProfileRequest request) {
        return studentService.updateMyProfile(request);
    }

    @GetMapping("/me/graduation-eligibility")
    public GraduationEligibilityResponse checkGraduationEligibility() {
        return studentService.checkGraduationEligibility();
    }
}