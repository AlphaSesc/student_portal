package com.example.student_portal.repository;

import com.example.student_portal.entity.Student;
import com.example.student_portal.entity.PortalUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Repository for managing student profile data
public interface StudentRepository extends JpaRepository<Student, Long> {

    // Retrieves student profile associated with a specific portal user (used after authentication)
    Optional<Student> findByPortalUser(PortalUser portalUser);

}