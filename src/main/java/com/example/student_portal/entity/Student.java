package com.example.student_portal.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "students")
// Represents student profile information linked to a portal user account
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false, unique = true)
    // Unique identifier used across microservices (e.g., Finance, Library)
    private String studentId;

    private String firstName;

    private String lastName;

    private String phone;

    @Column(length = 500)
    private String address;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portal_user_id", nullable = false, unique = true)
    // Establishes a one-to-one relationship with PortalUser for authentication.
    // Each student profile is linked to exactly one user account.
    // LAZY loading is used to avoid fetching authentication data unless required.
    private PortalUser portalUser;
}