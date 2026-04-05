package com.example.student_portal.client;

import com.example.student_portal.dto.finance.CreateFinanceAccountRequest;
import com.example.student_portal.dto.finance.CreateInvoiceRequest;
import com.example.student_portal.dto.finance.OutstandingBalanceResponse;
import com.example.student_portal.exception.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class FinanceClient {

    private final RestTemplate restTemplate;

    @Value("${finance.service.base-url}")
    private String financeBaseUrl;

    public void createAccount(CreateFinanceAccountRequest request) {
        try {
            restTemplate.postForObject(
                    financeBaseUrl + "/api/accounts",
                    request,
                    Void.class
            );
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to create finance account");
        }
    }

    public void createInvoice(CreateInvoiceRequest request) {
        try {
            restTemplate.postForObject(
                    financeBaseUrl + "/api/invoices",
                    request,
                    Void.class
            );
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to create invoice in finance service");
        }
    }

    public OutstandingBalanceResponse checkOutstandingBalance(String studentId) {
        try {
            return restTemplate.getForObject(
                    financeBaseUrl + "/api/invoices/outstanding/" + studentId,
                    OutstandingBalanceResponse.class
            );
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to check outstanding balance from finance service");
        }
    }
}