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

@Service
@RequiredArgsConstructor
public class FinanceService {

    private final FinanceClient financeClient;
    private final AuthenticatedUserService authenticatedUserService;
    private final StudentRepository studentRepository;

    public PayInvoiceResponse payInvoice(StudentPayInvoiceRequest request) {
        PortalUser portalUser = authenticatedUserService.getCurrentStudentUser();

        Student student = studentRepository.findByPortalUser(portalUser)
                .orElseThrow(() -> new UserNotFoundException("Student profile not found"));

        PayInvoiceRequest payInvoiceRequest = new PayInvoiceRequest();
        payInvoiceRequest.setStudentId(student.getStudentId());
        payInvoiceRequest.setReference(request.getReference());

        return financeClient.payInvoice(payInvoiceRequest);
    }
}