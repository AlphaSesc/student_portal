package com.example.student_portal.repository;

import com.example.student_portal.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Repository for managing course data and lookup operations
public interface CourseRepository extends JpaRepository<Course, Long> {

    // Retrieves course using unique courseCode
    Optional<Course> findByCourseCode(String courseCode);
}