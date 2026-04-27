package com.example.student_portal.service;

import com.example.student_portal.entity.Course;
import com.example.student_portal.exception.ResourceNotFoundException;
import com.example.student_portal.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseService courseService;

    private Course course;

    @BeforeEach
    void setUp() {
        course = Course.builder()
                .id(1L)
                .courseCode("CS101")
                .title("Distributed Systems")
                .description("Service-based architecture")
                .fee(BigDecimal.valueOf(2500))
                .build();
    }

    @Test
    void getAllCoursesShouldReturnCourseList() {
        Course secondCourse = Course.builder()
                .id(2L)
                .courseCode("SE202")
                .title("Software Engineering")
                .description("Software design and testing")
                .fee(BigDecimal.valueOf(1800))
                .build();

        when(courseRepository.findAll()).thenReturn(List.of(course, secondCourse));

        List<Course> courses = courseService.getAllCourses();

        assertEquals(2, courses.size());
        assertEquals("CS101", courses.get(0).getCourseCode());
        assertEquals("SE202", courses.get(1).getCourseCode());
    }

    @Test
    void getCourseByIdShouldReturnCourseWhenExists() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        Course result = courseService.getCourseById(1L);

        assertSame(course, result);
        assertEquals("Distributed Systems", result.getTitle());
    }

    @Test
    void getCourseByIdShouldThrowWhenCourseDoesNotExist() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> courseService.getCourseById(99L)
        );

        assertEquals("Course not found with id: 99", exception.getMessage());
    }

    @Test
    void getByCourseCodeShouldReturnCourseWhenExists() {
        when(courseRepository.findByCourseCode("CS101")).thenReturn(Optional.of(course));

        Course result = courseService.getByCourseCode("CS101");

        assertSame(course, result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getByCourseCodeShouldThrowWhenCourseDoesNotExist() {
        when(courseRepository.findByCourseCode("MISSING")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> courseService.getByCourseCode("MISSING")
        );

        assertEquals("Course not found with code: MISSING", exception.getMessage());
    }

    @Test
    void createCourseShouldSaveAndReturnCourse() {
        when(courseRepository.save(course)).thenReturn(course);

        Course result = courseService.createCourse(course);

        assertSame(course, result);
        verify(courseRepository).save(course);
    }

    @Test
    void updateCourseShouldUpdateExistingCourse() {
        Course request = Course.builder()
                .courseCode("CS999")
                .title("Advanced Microservices")
                .description("Updated description")
                .fee(BigDecimal.valueOf(3000))
                .build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(courseRepository.save(course)).thenReturn(course);

        Course result = courseService.updateCourse(1L, request);

        assertEquals("CS999", result.getCourseCode());
        assertEquals("Advanced Microservices", result.getTitle());
        assertEquals("Updated description", result.getDescription());
        assertEquals(BigDecimal.valueOf(3000), result.getFee());
        verify(courseRepository).save(course);
    }

    @Test
    void updateCourseShouldThrowWhenCourseDoesNotExist() {
        Course request = Course.builder()
                .courseCode("CS999")
                .title("Advanced Microservices")
                .description("Updated description")
                .fee(BigDecimal.valueOf(3000))
                .build();

        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> courseService.updateCourse(99L, request)
        );

        assertEquals("Course not found with id: 99", exception.getMessage());
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void deleteCourseShouldDeleteWhenCourseExists() {
        when(courseRepository.existsById(1L)).thenReturn(true);

        courseService.deleteCourse(1L);

        verify(courseRepository).deleteById(1L);
    }

    @Test
    void deleteCourseShouldThrowWhenCourseDoesNotExist() {
        when(courseRepository.existsById(99L)).thenReturn(false);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> courseService.deleteCourse(99L)
        );

        assertEquals("Course not found with id: 99", exception.getMessage());
        verify(courseRepository, never()).deleteById(anyLong());
    }
}