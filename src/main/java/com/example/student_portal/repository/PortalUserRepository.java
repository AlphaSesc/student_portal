package com.example.student_portal.repository;

import com.example.student_portal.entity.PortalUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PortalUserRepository extends JpaRepository<PortalUser, Long> {

    Optional<PortalUser> findByEmail(String email);
//    Optional<PortalUser> findByUsername(String username);
}
