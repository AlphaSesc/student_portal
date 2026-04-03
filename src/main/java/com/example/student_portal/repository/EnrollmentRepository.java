package com.example.student_portal.repository;

import com.example.student_portal.entity.Enrollment;
import com.example.student_portal.entity.Student;
import com.example.student_portal.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudent(Student student);

    Optional<Enrollment> findByStudentAndCourse(Student student, Course course);
}