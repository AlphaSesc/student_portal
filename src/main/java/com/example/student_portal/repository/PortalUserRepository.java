package com.example.student_portal.repository;

import com.example.student_portal.entity.PortalUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Repository for handling PortalUser persistence and lookup operations
public interface PortalUserRepository extends JpaRepository<PortalUser, Long> {

    // Retrieves user by email (used for authentication and login)
    Optional<PortalUser> findByEmail(String email);

}
