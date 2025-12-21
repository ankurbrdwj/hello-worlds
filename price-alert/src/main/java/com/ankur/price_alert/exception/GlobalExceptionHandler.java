package com.ankur.price_alert.exception;

import com.ankur.price_alert.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Global exception handler for REST controllers.
 * Follows Single Responsibility Principle - centralized exception handling.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.of(
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI(),
                Map.of(
                        "resourceType", ex.getResourceType(),
                        "resourceId", ex.getResourceId()
                )
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(AlertQuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleAlertQuotaExceeded(
            AlertQuotaExceededException ex, HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.of(
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI(),
                Map.of(
                        "userId", ex.getUserId(),
                        "currentCount", ex.getCurrentCount(),
                        "maxAllowed", ex.getMaxAllowed()
                )
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex, HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.of(
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI(),
                Map.of(
                        "resourceType", ex.getResourceType(),
                        "identifier", ex.getIdentifier()
                )
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(InvalidAlertConfigurationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAlertConfig(
            InvalidAlertConfigurationException ex, HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.of(
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(PriceAlertException.class)
    public ResponseEntity<ErrorResponse> handlePriceAlertException(
            PriceAlertException ex, HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.of(
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.of(
                "INVALID_ARGUMENT",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.of(
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                request.getRequestURI()
        );

        // Log the actual error for debugging
        System.err.println("Unexpected error: " + ex.getMessage());
        ex.printStackTrace();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
