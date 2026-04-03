package com.example.student_portal.service;

import com.example.student_portal.dto.EnrollmentRequest;
import com.example.student_portal.dto.EnrollmentResponse;
import com.example.student_portal.entity.Course;
import com.example.student_portal.entity.Enrollment;
import com.example.student_portal.entity.PortalUser;
import com.example.student_portal.entity.Student;
import com.example.student_portal.exception.BusinessException;
import com.example.student_portal.exception.ResourceAlreadyExistsException;
import com.example.student_portal.exception.ResourceNotFoundException;
import com.example.student_portal.exception.UserNotFoundException;
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

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final PortalUserRepository portalUserRepository;

    public EnrollmentResponse enroll(EnrollmentRequest request) {
        PortalUser portalUser = getLoggedInPortalUser();

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

    private Student createStudentForFirstEnrollment(PortalUser portalUser) {
        Student student = Student.builder()
                .studentId(StudentIdGenerator.generate())
                .portalUser(portalUser)
                .build();

        return studentRepository.save(student);
    }

    private PortalUser getLoggedInPortalUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new UserNotFoundException("Authenticated user not found");
        }

        String email = authentication.getName();

        return portalUserRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}