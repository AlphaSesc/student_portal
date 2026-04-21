package com.example.student_portal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;

@Entity
@Table(name = "courses")
// Represents a course available for enrollment in the student portal
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Primary key for course entity
    private Long id;

    @Column(name = "course_code", nullable = false, unique = true)
    // Unique course identifier used for enrollment and invoice creation
    private String courseCode;

    @Column(nullable = false)
    // Course title displayed to students
    private String title;

    @Column(length = 1000)
    // Detailed description of the course content
    private String description;

    @Column(nullable = false)
    // Course fee used when generating enrollment invoice in Finance service
    private BigDecimal fee;
}