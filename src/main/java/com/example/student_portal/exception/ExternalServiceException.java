package com.example.student_portal.exception;

import org.springframework.http.HttpStatus;

public class ExternalServiceException extends BusinessException {

    public ExternalServiceException(String message) {
        super(message, HttpStatus.BAD_GATEWAY);
    }
}