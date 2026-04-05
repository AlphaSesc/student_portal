package com.example.student_portal.service;

import com.example.student_portal.client.FinanceClient;
import com.example.student_portal.dto.GraduationEligibilityResponse;
import com.example.student_portal.dto.StudentProfileResponse;
import com.example.student_portal.dto.UpdateStudentProfileRequest;
import com.example.student_portal.entity.PortalUser;
import com.example.student_portal.entity.Student;
import com.example.student_portal.entity.UserRole;
import com.example.student_portal.exception.ResourceNotFoundException;
import com.example.student_portal.exception.UnauthorizedOperationException;
import com.example.student_portal.exception.UserNotFoundException;
import com.example.student_portal.repository.PortalUserRepository;
import com.example.student_portal.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final PortalUserRepository portalUserRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final FinanceClient financeClient;

    public StudentProfileResponse getMyProfile() {
        PortalUser portalUser = authenticatedUserService.getCurrentStudentUser();

        Student student = studentRepository.findByPortalUser(portalUser)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

        return mapToProfileResponse(student, portalUser);
    }

    public StudentProfileResponse updateMyProfile(UpdateStudentProfileRequest request) {
        PortalUser portalUser = authenticatedUserService.getCurrentStudentUser();

        Student student = studentRepository.findByPortalUser(portalUser)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setPhone(request.getPhone());
        student.setAddress(request.getAddress());

        Student updatedStudent = studentRepository.save(student);

        return mapToProfileResponse(updatedStudent, portalUser);
    }

    private StudentProfileResponse mapToProfileResponse(Student student, PortalUser portalUser) {
        return StudentProfileResponse.builder()
                .studentId(student.getStudentId())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .phone(student.getPhone())
                .address(student.getAddress())
                .email(portalUser.getEmail())
                .build();
    }

    public GraduationEligibilityResponse checkGraduationEligibility() {
        PortalUser portalUser = authenticatedUserService.getCurrentStudentUser();

        Student student = studentRepository.findByPortalUser(portalUser)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

        var balanceResponse = financeClient.checkOutstandingBalance(student.getStudentId());

        boolean eligible = !balanceResponse.isHasOutstandingBalance();

        return GraduationEligibilityResponse.builder()
                .studentId(student.getStudentId())
                .eligible(eligible)
                .message(eligible
                        ? "Student is eligible to graduate"
                        : "Student is not eligible to graduate due to outstanding invoices")
                .build();
    }

    }