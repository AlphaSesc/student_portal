package com.example.student_portal.dto.finance;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Request DTO used to communicate with Finance service to process invoice payment using studentId and reference
public class PayInvoiceRequest {

    private String studentId;
    private String reference;
}