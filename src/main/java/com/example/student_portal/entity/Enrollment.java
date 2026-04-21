package com.example.student_portal.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"student_id", "course_id"})
        })
// Represents the relationship between students and courses (many-to-many resolved via join entity)
// Ensures a student cannot enroll in the same course multiple times
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    // Links enrollment to a specific student (many enrollments per student)
    // LAZY loading avoids unnecessary fetching of student details
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    // Links enrollment to a specific course (many enrollments per course)
    // LAZY loading improves performance when course data is not needed
    private Course course;

    // Timestamp when the student enrolled in the course
    private LocalDateTime enrolledAt;
}