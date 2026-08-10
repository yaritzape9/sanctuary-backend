package com.sanctuary.sanctuary_backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.MediaType;
import lombok.extern.slf4j.Slf4j;
import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SightingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSightingNotFound(
            SightingNotFoundException ex, HttpServletRequest request) {
        log.warn("Sighting not found at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateConfirmationException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateConfirmation(
            DuplicateConfirmationException ex, HttpServletRequest request) {
        log.warn("Duplicate confirmation attempt at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }
    
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            org.springframework.web.servlet.resource.NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("No route found for [{}]", request.getRequestURI());
        return buildResponse(HttpStatus.NOT_FOUND, "Resource not found", request);
    }
    
    @ExceptionHandler(UnauthorizedSightingActionException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedSightingAction(
        UnauthorizedSightingActionException ex, HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at [{}]", request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    private ResponseEntity<ErrorResponse> buildResponse(
                HttpStatus status, String message, HttpServletRequest request) {
            ErrorResponse errorResponse = new ErrorResponse(
                    status.value(),
                    message,
                    Instant.now(),
                    request.getRequestURI()
            );
            return ResponseEntity.status(status)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(errorResponse);
    }
}