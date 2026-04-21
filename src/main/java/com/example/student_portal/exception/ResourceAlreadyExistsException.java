package com.example.student_portal.exception;

import org.springframework.http.HttpStatus;

// Thrown when attempting to create a resource that already exists (HTTP 409)
public class ResourceAlreadyExistsException extends BusinessException {
    public ResourceAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}