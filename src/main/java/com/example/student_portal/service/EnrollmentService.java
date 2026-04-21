package com.example.student_portal.service;

import com.example.student_portal.client.FinanceClient;
import com.example.student_portal.client.LibraryClient;
import com.example.student_portal.dto.EnrollmentRequest;
import com.example.student_portal.dto.EnrollmentResponse;
import com.example.student_portal.dto.finance.InvoiceType;
import com.example.student_portal.entity.*;
import com.example.student_portal.exception.*;
import com.example.student_portal.repository.CourseRepository;
import com.example.student_portal.repository.EnrollmentRepository;
import com.example.student_portal.repository.StudentRepository;
import com.example.student_portal.util.StudentIdGenerator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
// Service handling enrollment logic, including validation and integration with Finance and Library services
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final FinanceClient financeClient;
    private final LibraryClient libraryClient;

    @Transactional
    // Enrolls the currently authenticated student into a course
    public EnrollmentResponse enroll(EnrollmentRequest request) {

        // Get authenticated user and ensure they are a student
        PortalUser portalUser = authenticatedUserService.getCurrentStudentUser();

        // Fetch course to enroll in
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        // Fetch existing student profile or create one for first-time enrollment
        Student student = studentRepository.findByPortalUser(portalUser)
                .orElseGet(() -> createStudentForFirstEnrollment(portalUser));

        // Prevent duplicate enrollment
        enrollmentRepository.findByStudentAndCourse(student, course)
                .ifPresent(enrollment -> {
                    throw new ResourceAlreadyExistsException("Student is already enrolled in this course");
                });

        // Create enrollment record
        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .enrolledAt(LocalDateTime.now())
                .build();

        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);

        // Trigger invoice creation in Finance service
        financeClient.createInvoice(
                com.example.student_portal.dto.finance.CreateInvoiceRequest.builder()
                        .studentId(student.getStudentId())
                        .courseCode(course.getCourseCode())
                        .invoiceType(InvoiceType.COURSE_ENROLLMENT)
                        .amount(course.getFee())
                        .build()
        );

        // Return response DTO
        return EnrollmentResponse.builder()
                .enrollmentId(savedEnrollment.getId())
                .studentId(student.getStudentId())
                .courseId(course.getId())
                .courseCode(course.getCourseCode())
                .courseTitle(course.getTitle())
                .enrolledAt(savedEnrollment.getEnrolledAt())
                .build();
    }

    // Retrieves enrollments of the currently authenticated student
    public List<EnrollmentResponse> getMyEnrollments() {
        PortalUser portalUser = authenticatedUserService.getCurrentStudentUser();

        // Fetch student profile
        Student student = studentRepository.findByPortalUser(portalUser)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

        // Map enrollments to response DTOs
        return enrollmentRepository.findByStudent(student)
                .stream()
                .map(enrollment -> EnrollmentResponse.builder()
                        .enrollmentId(enrollment.getId())
                        .studentId(student.getStudentId())
                        .courseId(enrollment.getCourse().getId())
                        .courseCode(enrollment.getCourse().getCourseCode())
                        .courseTitle(enrollment.getCourse().getTitle())
                        .enrolledAt(enrollment.getEnrolledAt())
                        .build())
                .toList();
    }

    // Creates student profile during first enrollment and registers in external services
    private Student createStudentForFirstEnrollment(PortalUser portalUser) {
        Student student = Student.builder()
                .studentId(StudentIdGenerator.generate())
                .portalUser(portalUser)
                .build();

        Student savedStudent = studentRepository.save(student);

        // Create finance account for student
        financeClient.createAccount(
                com.example.student_portal.dto.finance.CreateFinanceAccountRequest.builder()
                        .studentId(savedStudent.getStudentId())
                        .email(portalUser.getEmail())
                        .build()
        );

        // Register student in library system
        libraryClient.registerStudent(
                com.example.student_portal.dto.library.CreateLibraryAccountRequest.builder()
                        .studentId(savedStudent.getStudentId())
                        .build()
        );

        return savedStudent;
    }

    }