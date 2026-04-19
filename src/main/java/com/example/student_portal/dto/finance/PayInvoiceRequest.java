package com.example.student_portal.dto.finance;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayInvoiceRequest {

    private String studentId;
    private String reference;
}