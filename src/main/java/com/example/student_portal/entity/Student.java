package com.example.student_portal.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "students")
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
    private String studentId;

    private String firstName;

    private String lastName;

    private String phone;

    @Column(length = 500)
    private String address;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portal_user_id", nullable = false, unique = true)
    private PortalUser portalUser;
}