package com.example.student_portal.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProfileResponse {

    private String studentId;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String email;
}