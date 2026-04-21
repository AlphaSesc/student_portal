package com.example.student_portal.controller;

import com.example.student_portal.dto.finance.PayInvoiceResponse;
import com.example.student_portal.dto.finance.StudentPayInvoiceRequest;
import com.example.student_portal.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
// REST controller handling student finance operations via Finance service
public class FinanceController {

    private final FinanceService financeService;

    @PostMapping("/pay")
    // Processes invoice payment for the authenticated student
    public PayInvoiceResponse payInvoice(@RequestBody StudentPayInvoiceRequest request) {
        return financeService.payInvoice(request);
    }

    // Retrieves all invoices of the authenticated student
    @GetMapping("/my-invoices")
    public List<PayInvoiceResponse> getMyInvoices() {
        return financeService.getMyInvoices();
    }
}