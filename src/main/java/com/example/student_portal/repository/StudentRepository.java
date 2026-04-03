package com.example.student_portal.repository;

import com.example.student_portal.entity.Student;
import com.example.student_portal.entity.PortalUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByPortalUser(PortalUser portalUser);

    Optional<Student> findByStudentId(String studentId);
}