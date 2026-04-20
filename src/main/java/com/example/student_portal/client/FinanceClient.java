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
        } catch (HttpStatusCodeException ex) {
            throw new ExternalServiceException(extractErrorMessage(ex));
        }

        catch (RestClientException ex) {
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
        } catch (HttpStatusCodeException ex) {
            throw new ExternalServiceException(extractErrorMessage(ex));
        }

        catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to create invoice in finance service");
        }
    }

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