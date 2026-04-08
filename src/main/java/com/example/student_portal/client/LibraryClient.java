package com.example.student_portal.client;

import com.example.student_portal.dto.library.CreateLibraryAccountRequest;
import com.example.student_portal.exception.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class LibraryClient {

    private final RestTemplate restTemplate;

    @Value("${library.service.base-url}")
    private String libraryBaseUrl;

    public void registerStudent(CreateLibraryAccountRequest request) {
        try {
            restTemplate.postForObject(
                    libraryBaseUrl + "/api/register",
                    request,
                    Void.class
            );
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to create library account");
        }
    }
}