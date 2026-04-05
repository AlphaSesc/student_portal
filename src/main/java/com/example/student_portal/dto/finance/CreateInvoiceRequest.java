package com.example.student_portal.dto.finance;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInvoiceRequest {
    private String studentId;
    private String courseCode;
    private BigDecimal amount;
}