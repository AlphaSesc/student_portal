package com.example.student_portal.dto.finance;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Response DTO received from Finance service indicating whether a student has outstanding invoices
public class OutstandingBalanceResponse {
    private String studentId;
    private boolean hasOutstandingBalance;
}