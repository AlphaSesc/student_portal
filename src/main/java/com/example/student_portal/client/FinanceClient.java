package com.example.student_portal.client;

import com.example.student_portal.dto.ApiErrorResponse;
import com.example.student_portal.dto.finance.*;
import com.example.student_portal.exception.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
// Client responsible for communicating with Finance microservice
public class FinanceClient {

    private final RestTemplate restTemplate;

    @Value("${finance.service.base-url}")
    // Base URL of Finance service (configured externally)
    private String financeBaseUrl;

    // Creates finance account when a new student registers or when student Enrolls
    public void createAccount(CreateFinanceAccountRequest request) {
        try {
            restTemplate.postForObject(
                    financeBaseUrl + "/api/accounts",
                    request,
                    Void.class
            );
        } catch (HttpStatusCodeException ex) {
            // Extract meaningful error from finance service response
            throw new ExternalServiceException(extractErrorMessage(ex));
        }

        catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to create finance account");
        }
    }

    // Generates invoice (e.g., course enrollment or fine)
    public void createInvoice(CreateInvoiceRequest request) {
        try {
            restTemplate.postForObject(
                    financeBaseUrl + "/api/invoices",
                    request,
                    Void.class
            );
        } catch (HttpStatusCodeException ex) {
            throw new ExternalServiceException(extractErrorMessage(ex));
        }

        catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to create invoice in finance service");
        }
    }

    // Checks if student has unpaid invoices (used for eligibility logic)
    public OutstandingBalanceResponse checkOutstandingBalance(String studentId) {
        try {
            return restTemplate.getForObject(
                    financeBaseUrl + "/api/invoices/outstanding/" + studentId,
                    OutstandingBalanceResponse.class
            );
        }  catch (HttpStatusCodeException ex) {
            throw new ExternalServiceException(extractErrorMessage(ex));
        }

        catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to check outstanding balance from finance service");
        }
    }

    // Sends payment request to finance service
    public PayInvoiceResponse payInvoice(PayInvoiceRequest request) {
        try {
            HttpEntity<PayInvoiceRequest> entity = new HttpEntity<>(request);

            ResponseEntity<PayInvoiceResponse> response = restTemplate.exchange(
                    financeBaseUrl + "/api/invoices/pay",
                    HttpMethod.PUT,
                    entity,
                    PayInvoiceResponse.class
            );

            return response.getBody();

        } catch (HttpStatusCodeException ex) {
            throw new ExternalServiceException(extractErrorMessage(ex));
        }
        catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to pay invoice in finance service");
        }
    }

    // Retrieves all invoices for a student (used for invoice history UI)
    public PayInvoiceResponse[] getInvoicesByStudentId(String studentId) {
        try {
            return restTemplate.getForObject(
                    financeBaseUrl + "/api/invoices/student/" + studentId,
                    PayInvoiceResponse[].class
            );
        } catch (HttpStatusCodeException ex) {
            throw new ExternalServiceException(extractErrorMessage(ex));
        }
        catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to fetch invoices from finance service");
        }
    }

    // Extracts structured error message from Finance service response
    private String extractErrorMessage(HttpStatusCodeException ex) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ApiErrorResponse errorResponse = objectMapper.readValue(
                    ex.getResponseBodyAsString(),
                    ApiErrorResponse.class
            );

            if (errorResponse.getMessage() != null && !errorResponse.getMessage().isBlank()) {
                return errorResponse.getMessage();
            }
        } catch (Exception ignored) {
        }

        return "Finance service request failed";
    }
}