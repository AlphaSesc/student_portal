package com.example.student_portal.repository;

import com.example.student_portal.entity.Enrollment;
import com.example.student_portal.entity.Student;
import com.example.student_portal.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Repository for managing student-course enrollment relationships
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    // Retrieves all enrollments for a given student (used for enrollment history display)
    List<Enrollment> findByStudent(Student student);

    // Checks if a student is already enrolled in a specific course
    // Used to prevent duplicate enrollments
    Optional<Enrollment> findByStudentAndCourse(Student student, Course course);
}