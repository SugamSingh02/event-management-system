package com.eventmanagment.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex) {

        Map<String, Object> body = createBaseResponse(
                HttpStatus.NOT_FOUND,
                "Not Found"
        );

        body.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, Object> body = createBaseResponse(
                HttpStatus.BAD_REQUEST,
                "Validation Failed"
        );

        Map<String, String> errors = new LinkedHashMap<>();

        for (var error : ex.getBindingResult().getFieldErrors()) {
            errors.put(
                    error.getField(),
                    error.getDefaultMessage()
            );
        }

        body.put("details", errors);

        return ResponseEntity
                .badRequest()
                .body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidJson(
            HttpMessageNotReadableException ex) {

        Map<String, Object> body = createBaseResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid Request Body"
        );

        body.put(
                "message",
                "Request body is missing, malformed, or contains invalid values"
        );

        return ResponseEntity
                .badRequest()
                .body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex) {

        Map<String, Object> body = createBaseResponse(
                HttpStatus.FORBIDDEN,
                "Forbidden"
        );

        body.put(
                "message",
                "You do not have permission to perform this action"
        );

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(body);
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            IllegalStateException.class
    })
    public ResponseEntity<Map<String, Object>> handleBusinessExceptions(
            RuntimeException ex) {

        Map<String, Object> body = createBaseResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request"
        );

        body.put("message", ex.getMessage());

        return ResponseEntity
                .badRequest()
                .body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {

        Map<String, Object> body = createBaseResponse(
                HttpStatus.CONFLICT,
                "Conflict"
        );

        body.put(
                "message",
                "The request conflicts with existing database data"
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(
            Exception ex) {

        log.error("Unhandled exception", ex);

        Map<String, Object> body = createBaseResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error"
        );

        body.put(
                "message",
                "An unexpected error occurred"
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }

    private Map<String, Object> createBaseResponse(
            HttpStatus status,
            String error) {

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", error);

        return body;
    }
}