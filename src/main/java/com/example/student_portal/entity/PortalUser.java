package com.example.student_portal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "portal_users")
// Represents a user of the student portal responsible for authentication and role-based access
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortalUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    // Unique email used as login credential
    private String email;

    @Column(nullable = false)
    // Encrypted password for authentication
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    // Defines user role for authorization (e.g., STUDENT, ADMIN)
    private UserRole role;
}
