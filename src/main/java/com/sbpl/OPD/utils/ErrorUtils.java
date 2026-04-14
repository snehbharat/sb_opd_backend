package com.sbpl.OPD.utils;

import com.sbpl.OPD.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Utility class for standardized error handling and validation
 */
public class ErrorUtils {
    
    // Common error messages
    public static final String RECORD_NOT_FOUND = "Record not found";
    public static final String INVALID_INPUT = "Invalid input provided";
    public static final String UNAUTHORIZED_ACCESS = "Unauthorized access";
    public static final String FORBIDDEN_ACTION = "Insufficient permissions";
    public static final String DUPLICATE_RECORD = "Record already exists";
    public static final String DATABASE_ERROR = "Database operation failed";
    public static final String VALIDATION_ERROR = "Validation failed";
    
    /**
     * Throw business exception for not found records
     */
    public static void throwNotFound(String entityName, Object identifier) {
        String message = String.format("%s not found with identifier: %s", entityName, identifier);
        throw new BusinessException(message, HttpStatus.NOT_FOUND, "NOT_FOUND");
    }
    
    /**
     * Throw business exception for duplicate records
     */
    public static void throwDuplicate(String entityName, String field, Object value) {
        String message = String.format("%s already exists with %s: %s", entityName, field, value);
        throw new BusinessException(message, HttpStatus.CONFLICT, "DUPLICATE_RECORD");
    }
    
    /**
     * Throw business exception for validation errors
     */
    public static void throwValidation(String message) {
        throw new BusinessException(message, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
    }
    
    /**
     * Throw business exception for authorization errors
     */
    public static void throwUnauthorized(String message) {
        throw new BusinessException(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
    
    /**
     * Throw business exception for forbidden actions
     */
    public static void throwForbidden(String message) {
        throw new BusinessException(message, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }
    
    /**
     * Throw business exception for database errors
     */
    public static void throwDatabaseError(String operation, Exception cause) {
        String message = String.format("Database error during %s: %s", operation, cause.getMessage());
        throw new BusinessException(message, HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE_ERROR");
    }
}