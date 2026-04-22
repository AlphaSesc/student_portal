package com.example.student_portal.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
// Base class for all business-related exceptions in the Student service.
// Allows consistent error handling by associating each exception with an HTTP status.
public abstract class BusinessException extends RuntimeException {

    private final HttpStatus status;

    protected BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

}