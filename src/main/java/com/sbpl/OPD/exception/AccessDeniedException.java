package com.sbpl.OPD.exception;

import org.springframework.http.HttpStatus;

/**
 * Custom exception for access denied scenarios.
 */
public class AccessDeniedException extends org.springframework.security.access.AccessDeniedException {

    private final HttpStatus httpStatus;

    public AccessDeniedException(String message) {
        super(message);
        this.httpStatus = HttpStatus.FORBIDDEN;
    }

    public AccessDeniedException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}