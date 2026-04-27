package com.example.student_portal.service;

import com.example.student_portal.client.FinanceClient;
import com.example.student_portal.dto.GraduationEligibilityResponse;
import com.example.student_portal.dto.StudentProfileResponse;
import com.example.student_portal.dto.UpdateStudentProfileRequest;
import com.example.student_portal.dto.finance.OutstandingBalanceResponse;
import com.example.student_portal.entity.PortalUser;
import com.example.student_portal.entity.Student;
import com.example.student_portal.entity.UserRole;
import com.example.student_portal.exception.ResourceNotFoundException;
import com.example.student_portal.repository.PortalUserRepository;
import com.example.student_portal.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private PortalUserRepository portalUserRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private FinanceClient financeClient;

    @InjectMocks
    private StudentService studentService;

    private PortalUser portalUser;
    private Student student;

    @BeforeEach
    void setUp() {
        portalUser = PortalUser.builder()
                .id(1L)
                .email("student@example.com")
                .password("encoded")
                .role(UserRole.STUDENT)
                .build();

        student = Student.builder()
                .id(10L)
                .studentId("STU-1000")
                .firstName("John")
                .lastName("Doe")
                .phone("9800000000")
                .address("Kathmandu")
                .portalUser(portalUser)
                .build();
    }

    @Test
    void getMyProfileShouldReturnMappedProfile() {
        when(authenticatedUserService.getCurrentStudentUser()).thenReturn(portalUser);
        when(studentRepository.findByPortalUser(portalUser)).thenReturn(Optional.of(student));

        StudentProfileResponse response = studentService.getMyProfile();

        assertEquals("STU-1000", response.getStudentId());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals("9800000000", response.getPhone());
        assertEquals("Kathmandu", response.getAddress());
        assertEquals("student@example.com", response.getEmail());
    }

    @Test
    void updateMyProfileShouldPersistChanges() {
        UpdateStudentProfileRequest request = UpdateStudentProfileRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .phone("9811111111")
                .address("Lalitpur")
                .build();

        when(authenticatedUserService.getCurrentStudentUser()).thenReturn(portalUser);
        when(studentRepository.findByPortalUser(portalUser)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentProfileResponse response = studentService.updateMyProfile(request);

        assertEquals("Jane", response.getFirstName());
        assertEquals("Smith", response.getLastName());
        assertEquals("9811111111", response.getPhone());
        assertEquals("Lalitpur", response.getAddress());
        verify(studentRepository).save(student);
    }

    @Test
    void getMyProfileShouldThrowWhenStudentProfileDoesNotExist() {
        when(authenticatedUserService.getCurrentStudentUser()).thenReturn(portalUser);
        when(studentRepository.findByPortalUser(portalUser)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> studentService.getMyProfile()
        );

        assertEquals("Student profile not found", exception.getMessage());
    }

    @Test
    void checkGraduationEligibilityShouldReturnEligibleWhenNoOutstandingBalance() {
        when(authenticatedUserService.getCurrentStudentUser()).thenReturn(portalUser);
        when(studentRepository.findByPortalUser(portalUser)).thenReturn(Optional.of(student));
        when(financeClient.checkOutstandingBalance("STU-1000")).thenReturn(
                OutstandingBalanceResponse.builder()
                        .studentId("STU-1000")
                        .hasOutstandingBalance(false)
                        .build()
        );

        GraduationEligibilityResponse response = studentService.checkGraduationEligibility();

        assertTrue(response.isEligible());
        assertEquals("Student is eligible to graduate", response.getMessage());
    }

    @Test
    void checkGraduationEligibilityShouldReturnNotEligibleWhenOutstandingBalanceExists() {
        when(authenticatedUserService.getCurrentStudentUser()).thenReturn(portalUser);
        when(studentRepository.findByPortalUser(portalUser)).thenReturn(Optional.of(student));
        when(financeClient.checkOutstandingBalance("STU-1000")).thenReturn(
                OutstandingBalanceResponse.builder()
                        .studentId("STU-1000")
                        .hasOutstandingBalance(true)
                        .build()
        );

        GraduationEligibilityResponse response = studentService.checkGraduationEligibility();

        assertFalse(response.isEligible());
        assertEquals("Student is not eligible to graduate due to outstanding invoices", response.getMessage());
    }

    @Test
    void updateMyProfileShouldThrowWhenStudentProfileDoesNotExist() {
        UpdateStudentProfileRequest request = UpdateStudentProfileRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .phone("9811111111")
                .address("Lalitpur")
                .build();

        when(authenticatedUserService.getCurrentStudentUser()).thenReturn(portalUser);
        when(studentRepository.findByPortalUser(portalUser)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> studentService.updateMyProfile(request)
        );

        assertEquals("Student profile not found", exception.getMessage());
    }

    @Test
    void checkGraduationEligibilityShouldThrowWhenStudentProfileDoesNotExist() {
        when(authenticatedUserService.getCurrentStudentUser()).thenReturn(portalUser);
        when(studentRepository.findByPortalUser(portalUser)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> studentService.checkGraduationEligibility()
        );

        assertEquals("Student profile not found", exception.getMessage());
    }
}
