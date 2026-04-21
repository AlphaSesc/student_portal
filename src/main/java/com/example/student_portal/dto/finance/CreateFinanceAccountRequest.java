package com.example.student_portal.dto.finance;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Request DTO used to communicate with Finance service for account creation
public class CreateFinanceAccountRequest {
    private String studentId;
    private String email;
}