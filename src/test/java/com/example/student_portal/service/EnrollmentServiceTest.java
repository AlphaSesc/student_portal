package com.example.student_portal.service;

import com.example.student_portal.client.FinanceClient;
import com.example.student_portal.client.LibraryClient;
import com.example.student_portal.dto.EnrollmentRequest;
import com.example.student_portal.dto.EnrollmentResponse;
import com.example.student_portal.dto.finance.CreateFinanceAccountRequest;
import com.example.student_portal.dto.finance.CreateInvoiceRequest;
import com.example.student_portal.dto.finance.InvoiceType;
import com.example.student_portal.dto.library.CreateLibraryAccountRequest;
import com.example.student_portal.entity.Course;
import com.example.student_portal.entity.Enrollment;
import com.example.student_portal.entity.PortalUser;
import com.example.student_portal.entity.Student;
import com.example.student_portal.entity.UserRole;
import com.example.student_portal.exception.ResourceAlreadyExistsException;
import com.example.student_portal.exception.ResourceNotFoundException;
import com.example.student_portal.repository.CourseRepository;
import com.example.student_portal.repository.EnrollmentRepository;
import com.example.student_portal.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private FinanceClient financeClient;

    @Mock
    private LibraryClient libraryClient;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private PortalUser portalUser;
    private Student student;
    private Course course;

    @BeforeEach
    void setUp() {
        portalUser = PortalUser.builder()
                .id(1L)
                .email("student@example.com")
                .password("encoded")
                .role(UserRole.STUDENT)
                .build();

        student = Student.builder()
                .id(1L)
                .studentId("STU-2001")
                .portalUser(portalUser)
                .build();

        course = Course.builder()
                .id(99L)
                .courseCode("CS101")
                .title("Distributed Systems")
                .description("Service orchestration and APIs")
                .fee(BigDecimal.valueOf(2500))
                .build();
    }

    @Test
    void enrollShouldCreateEnrollmentForExistingStudent() {
        EnrollmentRequest request = EnrollmentRequest.builder()
                .courseId(99L)
                .build();
        Enrollment savedEnrollment = Enrollment.builder()
                .id(10L)
                .student(student)
                .course(course)
                .enrolledAt(LocalDateTime.now())
                .build();

        when(authenticatedUserService.getCurrentStudentUser()).thenReturn(portalUser);
        when(courseRepository.findById(99L)).thenReturn(Optional.of(course));
        when(studentRepository.findByPortalUser(portalUser)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByStudentAndCourse(student, course)).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(savedEnrollment);

        EnrollmentResponse response = enrollmentService.enroll(request);

        ArgumentCaptor<CreateInvoiceRequest> invoiceCaptor = ArgumentCaptor.forClass(CreateInvoiceRequest.class);
        verify(financeClient).createInvoice(invoiceCaptor.capture());

        assertEquals(10L, response.getEnrollmentId());
        assertEquals("STU-2001", response.getStudentId());
        assertEquals("CS101", response.getCourseCode());
        assertEquals(InvoiceType.COURSE_ENROLLMENT, invoiceCaptor.getValue().getInvoiceType());
        assertEquals(BigDecimal.valueOf(2500), invoiceCaptor.getValue().getAmount());
        verify(financeClient, never()).createAccount(any(CreateFinanceAccountRequest.class));
        verify(libraryClient, never()).registerStudent(any(CreateLibraryAccountRequest.class));
    }

    @Test
    void enrollShouldCreateStudentAndExternalAccountsOnFirstEnrollment() {
        EnrollmentRequest request = EnrollmentRequest.builder()
                .courseId(99L)
                .build();
        Student savedStudent = Student.builder()
                .id(5L)
                .studentId("STU-NEW01")
                .portalUser(portalUser)
                .build();
        Enrollment savedEnrollment = Enrollment.builder()
                .id(11L)
                .student(savedStudent)
                .course(course)
                .enrolledAt(LocalDateTime.now())
                .build();

        when(authenticatedUserService.getCurrentStudentUser()).thenReturn(portalUser);
        when(courseRepository.findById(99L)).thenReturn(Optional.of(course));
        when(studentRepository.findByPortalUser(portalUser)).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenReturn(savedStudent);
        when(enrollmentRepository.findByStudentAndCourse(savedStudent, course)).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(savedEnrollment);

        EnrollmentResponse response = enrollmentService.enroll(request);

        ArgumentCaptor<CreateFinanceAccountRequest> financeAccountCaptor =
                ArgumentCaptor.forClass(CreateFinanceAccountRequest.class);
        ArgumentCaptor<CreateLibraryAccountRequest> libraryAccountCaptor =
                ArgumentCaptor.forClass(CreateLibraryAccountRequest.class);

        verify(financeClient).createAccount(financeAccountCaptor.capture());
        verify(libraryClient).registerStudent(libraryAccountCaptor.capture());

        assertEquals(11L, response.getEnrollmentId());
        assertEquals("STU-NEW01", response.getStudentId());
        assertEquals("student@example.com", financeAccountCaptor.getValue().getEmail());
        assertEquals("STU-NEW01", financeAccountCaptor.getValue().getStudentId());
        assertEquals("STU-NEW01", libraryAccountCaptor.getValue().getStudentId());
    }

    @Test
    void enrollShouldRejectDuplicateEnrollment() {
        EnrollmentRequest request = EnrollmentRequest.builder()
                .courseId(99L)
                .build();
        Enrollment existingEnrollment = Enrollment.builder()
                .id(12L)
                .student(student)
                .course(course)
                .enrolledAt(LocalDateTime.now())
                .build();

        when(authenticatedUserService.getCurrentStudentUser()).thenReturn(portalUser);
        when(courseRepository.findById(99L)).thenReturn(Optional.of(course));
        when(studentRepository.findByPortalUser(portalUser)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByStudentAndCourse(student, course))
                .thenReturn(Optional.of(existingEnrollment));

        ResourceAlreadyExistsException exception = assertThrows(
                ResourceAlreadyExistsException.class,
                () -> enrollmentService.enroll(request)
        );

        assertEquals("Student is already enrolled in this course", exception.getMessage());
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
        verify(financeClient, never()).createInvoice(any(CreateInvoiceRequest.class));
    }

    @Test
    void getMyEnrollmentsShouldMapEnrollmentHistory() {
        Enrollment firstEnrollment = Enrollment.builder()
                .id(1L)
                .student(student)
                .course(course)
                .enrolledAt(LocalDateTime.now())
                .build();
        Enrollment secondEnrollment = Enrollment.builder()
                .id(2L)
                .student(student)
                .course(Course.builder()
                        .id(100L)
                        .courseCode("SE202")
                        .title("Software Engineering")
                        .fee(BigDecimal.valueOf(1800))
                        .build())
                .enrolledAt(LocalDateTime.now().minusDays(1))
                .build();

        when(authenticatedUserService.getCurrentStudentUser()).thenReturn(portalUser);
        when(studentRepository.findByPortalUser(portalUser)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByStudent(student)).thenReturn(List.of(firstEnrollment, secondEnrollment));

        List<EnrollmentResponse> responses = enrollmentService.getMyEnrollments();

        assertEquals(2, responses.size());
        assertEquals("CS101", responses.get(0).getCourseCode());
        assertEquals("Software Engineering", responses.get(1).getCourseTitle());
        assertNotNull(responses.get(0).getEnrolledAt());
    }

    @Test
    void enrollShouldThrowWhenCourseNotFound() {

        // Arrange
        EnrollmentRequest request = EnrollmentRequest.builder()
                .courseId(99L)
                .build();

        when(authenticatedUserService.getCurrentStudentUser()).thenReturn(portalUser);
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> enrollmentService.enroll(request)
        );

        // Verify message
        assertEquals("Course not found", exception.getMessage());

        // Verify no further interactions
        verify(studentRepository, never()).findByPortalUser(any());
        verify(enrollmentRepository, never()).save(any());
        verify(financeClient, never()).createInvoice(any());
        verify(libraryClient, never()).registerStudent(any());
    }

    @Test
    void getMyEnrollmentsShouldThrowWhenStudentNotFound() {

        // Arrange
        when(authenticatedUserService.getCurrentStudentUser()).thenReturn(portalUser);
        when(studentRepository.findByPortalUser(portalUser)).thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> enrollmentService.getMyEnrollments()
        );

        // Verify message (important for marks)
        assertEquals("Student profile not found", exception.getMessage());

        // Verify no further calls (clean testing practice)
        verify(enrollmentRepository, never()).findByStudent(any());
    }
}
