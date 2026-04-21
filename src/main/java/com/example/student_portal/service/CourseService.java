package com.example.student_portal.service;

import com.example.student_portal.entity.Course;
import com.example.student_portal.exception.ResourceNotFoundException;
import com.example.student_portal.repository.CourseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
// Service layer handling business logic related to course management
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    // Retrieves all available courses for listing in the portal
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    // Fetches a course by its database ID
    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
    }

    // Fetches course using unique courseCode (used in enrollment and cross-service operations)
    public Course getByCourseCode(String courseCode) {
        return courseRepository.findByCourseCode(courseCode)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with code: " + courseCode));
    }

    // Creates a new course entry (admin operation)
    public Course createCourse(Course course) {
        return courseRepository.save(course);
    }

    // Updates existing course details
    public Course updateCourse(Long id, Course request) {
        Course existingCourse = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));

        // Update fields with new values
        existingCourse.setCourseCode(request.getCourseCode());
        existingCourse.setTitle(request.getTitle());
        existingCourse.setDescription(request.getDescription());
        existingCourse.setFee(request.getFee());

        return courseRepository.save(existingCourse);
    }

    // Deletes a course if it exists
    public void deleteCourse(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new RuntimeException("Course not found with id: " + id);
        }
        courseRepository.deleteById(id);
    }
}