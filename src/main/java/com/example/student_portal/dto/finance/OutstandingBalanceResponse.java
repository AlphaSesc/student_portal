package com.example.student_portal.dto.finance;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutstandingBalanceResponse {
    private String studentId;
    private boolean hasOutstandingBalance;
}