package com.sbpl.OPD.exception;

import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.response.ResponseDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private BaseResponse baseResponse;

    /**
     * Handle custom AccessDeniedException and Spring Security's AccessDeniedException
     */
    @ExceptionHandler({AccessDeniedException.class, org.springframework.security.access.AccessDeniedException.class})
    public ResponseEntity<ResponseDto> handleAccessDeniedException(Exception ex) {
        return baseResponse.errorResponse(HttpStatus.FORBIDDEN, "Access denied: " + ex.getMessage());
    }

    /**
     * Handle authentication exceptions
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ResponseDto> handleAuthenticationException(AuthenticationException ex) {
        return baseResponse.errorResponse(HttpStatus.UNAUTHORIZED, "Authentication failed: " + ex.getMessage());
    }

    /**
     * Handle validation exceptions from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDto> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("validationErrors", errors);
        errorData.put("message", "Validation failed. Please check the input data.");
        
        return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Validation failed", errorData);
    }

    /**
     * Handle constraint violation exceptions
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResponseDto> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                ));
        
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("validationErrors", errors);
        errorData.put("message", "Constraint validation failed.");
        
        return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Constraint validation failed", errorData);
    }

    /**
     * Handle method argument type mismatch exceptions
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ResponseDto> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String errorMessage = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s", 
                ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());
        
        return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, errorMessage);
    }

    /**
     * Handle HTTP message not readable exceptions (e.g., malformed JSON)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseDto> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Invalid request format. Please check your JSON structure.");
    }

    /**
     * Handle DataIntegrityViolationException (database constraint violations)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ResponseDto> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        String errorMessage = parseDatabaseConstraintError(ex);
        return baseResponse.errorResponse(HttpStatus.CONFLICT, errorMessage);
    }

    /**
     * Handle IllegalArgumentException (thrown in service layer for business logic errors)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseDto> handleIllegalArgumentException(IllegalArgumentException ex) {
        return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handle custom BusinessException
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ResponseDto> handleBusinessException(BusinessException ex) {
        return baseResponse.errorResponse(ex.getStatus(), ex.getMessage());
    }

    /**
     * Handle RuntimeException (thrown in service layer for various errors)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ResponseDto> handleRuntimeException(RuntimeException ex) {
        // Log the exception for debugging
        ex.printStackTrace();
        
        String message = ex.getMessage();
        if (message == null || message.isEmpty()) {
            message = "An unexpected runtime error occurred. Please try again later.";
        }
        
        return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    /**
     * Handle general exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDto> handleGeneralException(Exception ex) {
        // Log the exception for debugging
        ex.printStackTrace();
        
        return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "An unexpected error occurred. Please try again later.");
    }

    /**
     * Handle file upload size exceeded exceptions
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ResponseDto> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        String errorMessage = "File size exceeds the maximum allowed limit. Please upload a smaller file.";
        return baseResponse.errorResponse(HttpStatus.PAYLOAD_TOO_LARGE, errorMessage);
    }

    /**
     * Handle IO exceptions
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ResponseDto> handleIOException(IOException ex) {
        return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "File processing error occurred. Please try again.");
    }

    /**
     * Parse database constraint violation errors to provide user-friendly messages
     * @param e DataIntegrityViolationException
     * @return User-friendly error message
     */
    private String parseDatabaseConstraintError(DataIntegrityViolationException e) {
        // Get the root cause message
        String message = getRootCauseMessage(e);
        
        // Handle unique constraint violations based on exception message
        if (message != null) {

            if (message.toLowerCase().contains("duplicate") && message.toLowerCase().contains("constraint")) {
                if (message.contains("idx_doctor_registration_no")) {
                    return "Registration number already exists. Please use a different registration number.";
                } else if (message.contains("doctor_email")) {
                    return "Doctor email already exists. Please use a different email address.";
                } else if (message.contains("phone_number")) {
                    return "Phone number already exists. Please use a different phone number.";
                } else if (message.contains("username")) {
                    return "Username already exists. Please use a different username.";
                } else if (message.contains("email")) {
                    return "Email already exists. Please use a different email address.";
                } else {
                    return "Duplicate entry detected. Please check your input values.";
                }
            }
            // Handle other constraint violations
            else if (message.toLowerCase().contains("foreign key")) {
                return "Referenced record does not exist. Please check your input data.";
            }
            else if (message.toLowerCase().contains("not-null") || message.toLowerCase().contains("null value")) {
                return "Required field is missing. Please provide all required information.";
            }
        }

        // Default message if we can't parse the specific constraint
        return "Data validation failed. Please check your input and try again.";
    }

    /**
     * Get the root cause message from an exception
     * @param e Exception to analyze
     * @return Root cause message or null if not found
     */
    private String getRootCauseMessage(Exception e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }
}