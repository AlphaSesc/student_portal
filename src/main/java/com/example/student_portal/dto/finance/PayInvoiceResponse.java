package com.example.student_portal.dto.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PayInvoiceResponse {
    private Long id;
    private String studentId;
    private String courseCode;
    private InvoiceType invoiceType;
    private BigDecimal amount;
    private String reference;
    private InvoiceStatus status;
    private LocalDateTime createdAt;
}
