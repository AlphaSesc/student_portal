package com.example.student_portal.service;

import com.example.student_portal.client.FinanceClient;
import com.example.student_portal.dto.finance.InvoiceStatus;
import com.example.student_portal.dto.finance.InvoiceType;
import com.example.student_portal.dto.finance.PayInvoiceRequest;
import com.example.student_portal.dto.finance.PayInvoiceResponse;
import com.example.student_portal.dto.finance.StudentPayInvoiceRequest;
import com.example.student_portal.entity.PortalUser;
import com.example.student_portal.entity.Student;
import com.example.student_portal.entity.UserRole;
import com.example.student_portal.exception.UserNotFoundException;
import com.example.student_portal.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

    @Mock
    private FinanceClient financeClient;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private FinanceService financeService;

    private PortalUser portalUser;
    private Student student;

    @BeforeEach
    void setUp() {
        portalUser = PortalUser.builder()
                .id(1L)
                .email("student@example.com")
                .password("encoded")
                .role(UserRole.STUDENT)
                .build();

        student = Student.builder()
                .id(1L)
                .studentId("STU-2024")
                .portalUser(portalUser)
                .build();
    }

    @Test
    void payInvoiceShouldSendStudentSpecificRequestToFinanceService() {
        StudentPayInvoiceRequest request = new StudentPayInvoiceRequest();
        request.setReference("INV-100");

        PayInvoiceResponse expectedResponse = PayInvoiceResponse.builder()
                .id(5L)
                .studentId("STU-2024")
                .courseCode("CS101")
                .invoiceType(InvoiceType.COURSE_ENROLLMENT)
                .amount(BigDecimal.valueOf(1500))
                .reference("INV-100")
                .status(InvoiceStatus.PAID)
                .createdAt(LocalDateTime.now())
                .build();

        when(authenticatedUserService.getCurrentStudentUser()).thenReturn(portalUser);
        when(studentRepository.findByPortalUser(portalUser)).thenReturn(Optional.of(student));
        when(financeClient.payInvoice(org.mockito.ArgumentMatchers.any(PayInvoiceRequest.class)))
                .thenReturn(expectedResponse);

        PayInvoiceResponse response = financeService.payInvoice(request);

        ArgumentCaptor<PayInvoiceRequest> captor = ArgumentCaptor.forClass(PayInvoiceRequest.class);
        verify(financeClient).payInvoice(captor.capture());

        assertSame(expectedResponse, response);
        assertEquals("STU-2024", captor.getValue().getStudentId());
        assertEquals("INV-100", captor.getValue().getReference());
    }

    @Test
    void getMyInvoicesShouldReturnEmptyListWhenFinanceServiceReturnsNull() {
        when(authenticatedUserService.getCurrentStudentUser()).thenReturn(portalUser);
        when(studentRepository.findByPortalUser(portalUser)).thenReturn(Optional.of(student));
        when(financeClient.getInvoicesByStudentId("STU-2024")).thenReturn(null);

        List<PayInvoiceResponse> invoices = financeService.getMyInvoices();

        assertTrue(invoices.isEmpty());
    }

    @Test
    void getMyInvoicesShouldReturnInvoiceListFromFinanceService() {
        PayInvoiceResponse firstInvoice = PayInvoiceResponse.builder()
                .id(1L)
                .reference("INV-1")
                .studentId("STU-2024")
                .build();
        PayInvoiceResponse secondInvoice = PayInvoiceResponse.builder()
                .id(2L)
                .reference("INV-2")
                .studentId("STU-2024")
                .build();

        when(authenticatedUserService.getCurrentStudentUser()).thenReturn(portalUser);
        when(studentRepository.findByPortalUser(portalUser)).thenReturn(Optional.of(student));
        when(financeClient.getInvoicesByStudentId("STU-2024"))
                .thenReturn(new PayInvoiceResponse[]{firstInvoice, secondInvoice});

        List<PayInvoiceResponse> invoices = financeService.getMyInvoices();

        assertIterableEquals(List.of(firstInvoice, secondInvoice), invoices);
    }

    @Test
    void payInvoiceShouldThrowWhenStudentProfileIsMissing() {
        StudentPayInvoiceRequest request = new StudentPayInvoiceRequest();
        request.setReference("INV-404");

        when(authenticatedUserService.getCurrentStudentUser()).thenReturn(portalUser);
        when(studentRepository.findByPortalUser(portalUser)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> financeService.payInvoice(request)
        );

        assertEquals("Student profile not found", exception.getMessage());
    }

    @Test
    void getMyInvoicesShouldThrowWhenStudentProfileIsMissing() {
        when(authenticatedUserService.getCurrentStudentUser()).thenReturn(portalUser);
        when(studentRepository.findByPortalUser(portalUser)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> financeService.getMyInvoices()
        );

        assertEquals("Student profile not found", exception.getMessage());
    }
}


