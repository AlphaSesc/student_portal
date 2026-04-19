package com.example.student_portal.controller;

import com.example.student_portal.dto.finance.PayInvoiceResponse;
import com.example.student_portal.dto.finance.StudentPayInvoiceRequest;
import com.example.student_portal.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    @PostMapping("/pay")
    public PayInvoiceResponse payInvoice(@RequestBody StudentPayInvoiceRequest request) {
        return financeService.payInvoice(request);
    }
}