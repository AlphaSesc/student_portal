package com.example.student_portal.service;

import com.example.student_portal.dto.EnrollmentRequest;
import com.example.student_portal.dto.EnrollmentResponse;
import com.example.student_portal.entity.*;
import com.example.student_portal.exception.*;
import com.example.student_portal.repository.CourseRepository;
import com.example.student_portal.repository.EnrollmentRepository;
import com.example.student_portal.repository.PortalUserRepository;
import com.example.student_portal.repository.StudentRepository;
import com.example.student_portal.util.StudentIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final PortalUserRepository portalUserRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public EnrollmentResponse enroll(EnrollmentRequest request) {
        PortalUser portalUser = authenticatedUserService.getCurrentStudentUser();;

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        Student student = studentRepository.findByPortalUser(portalUser)
                .orElseGet(() -> createStudentForFirstEnrollment(portalUser));

        enrollmentRepository.findByStudentAndCourse(student, course)
                .ifPresent(enrollment -> {
                    throw new ResourceAlreadyExistsException("Student is already enrolled in this course");
                });

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .enrolledAt(LocalDateTime.now())
                .build();

        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);

        return EnrollmentResponse.builder()
                .enrollmentId(savedEnrollment.getId())
                .studentId(student.getStudentId())
                .courseId(course.getId())
                .courseCode(course.getCourseCode())
                .courseTitle(course.getTitle())
                .enrolledAt(savedEnrollment.getEnrolledAt())
                .build();
    }

    public List<EnrollmentResponse> getMyEnrollments() {
        PortalUser portalUser = authenticatedUserService.getCurrentStudentUser();

        Student student = studentRepository.findByPortalUser(portalUser)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

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

    private Student createStudentForFirstEnrollment(PortalUser portalUser) {
        Student student = Student.builder()
                .studentId(StudentIdGenerator.generate())
                .portalUser(portalUser)
                .build();

        return studentRepository.save(student);
    }

    }