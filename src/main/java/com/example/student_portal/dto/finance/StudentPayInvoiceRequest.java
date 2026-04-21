package com.example.student_portal.dto.finance;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// Request DTO used by the student portal to initiate invoice payment using invoice reference
public class StudentPayInvoiceRequest {
    private String reference;
}