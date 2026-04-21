package com.example.student_portal.service;

import com.example.student_portal.client.FinanceClient;
import com.example.student_portal.dto.finance.PayInvoiceRequest;
import com.example.student_portal.dto.finance.PayInvoiceResponse;
import com.example.student_portal.dto.finance.StudentPayInvoiceRequest;
import com.example.student_portal.entity.PortalUser;
import com.example.student_portal.entity.Student;
import com.example.student_portal.exception.UserNotFoundException;
import com.example.student_portal.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
// Service responsible for handling student financial operations via Finance microservice
public class FinanceService {

    private final FinanceClient financeClient;
    private final AuthenticatedUserService authenticatedUserService;
    private final StudentRepository studentRepository;

    // Processes invoice payment for the authenticated student
    public PayInvoiceResponse payInvoice(StudentPayInvoiceRequest request) {

        // Get currently logged-in student
        PortalUser portalUser = authenticatedUserService.getCurrentStudentUser();

        Student student = studentRepository.findByPortalUser(portalUser)
                .orElseThrow(() -> new UserNotFoundException("Student profile not found"));

        // Prepare request for Finance service
        PayInvoiceRequest payInvoiceRequest = new PayInvoiceRequest();
        payInvoiceRequest.setStudentId(student.getStudentId());
        payInvoiceRequest.setReference(request.getReference());

        // Delegate payment processing to Finance microservice
        return financeClient.payInvoice(payInvoiceRequest);
    }

    // Retrieves all invoices for the authenticated student
    public List<PayInvoiceResponse> getMyInvoices() {
        PortalUser portalUser = authenticatedUserService.getCurrentStudentUser();

        Student student = studentRepository.findByPortalUser(portalUser)
                .orElseThrow(() -> new UserNotFoundException("Student profile not found"));

        // Fetch invoices from Finance service
        PayInvoiceResponse[] invoices = financeClient.getInvoicesByStudentId(student.getStudentId());

        // Convert array to list, handle null safely
        return invoices == null ? List.of() : Arrays.asList(invoices);
    }
}