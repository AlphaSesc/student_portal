package com.example.student_portal.client;

import com.example.student_portal.dto.ApiErrorResponse;
import com.example.student_portal.dto.library.CreateLibraryAccountRequest;
import com.example.student_portal.exception.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
// Client responsible for communicating with Library microservice
public class LibraryClient {

    private final RestTemplate restTemplate;

    @Value("${library.service.base-url}")
    // Base URL of Library service (configured externally)
    private String libraryBaseUrl;

    // Registers a student in the library system when student enrols in course
    public void registerStudent(CreateLibraryAccountRequest request) {
        try {
            restTemplate.postForObject(
                    libraryBaseUrl + "/api/library/register",
                    request,
                    Void.class
            );
        } catch (HttpStatusCodeException ex) {
            // Extract meaningful error returned by Library service
            throw new ExternalServiceException(extractErrorMessage(ex));
        }

        catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to create library account");
        }
    }

    // Extracts structured error message from Library service response
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

        return "Library service request failed";
    }
}
