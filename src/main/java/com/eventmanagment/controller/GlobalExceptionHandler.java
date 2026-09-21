package com.eventmanagment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // =========================
    // VALIDATION ERRORS
    // =========================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            MethodArgumentNotValidException exception) {

        Map<String, String> errors = new HashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errors);
    }

    // =========================
    // RUNTIME ERRORS
    // =========================

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(
            RuntimeException exception) {

        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            message = "Something went wrong";
        }

        HttpStatus status = determineStatus(message);

        Map<String, String> error = new HashMap<>();
        error.put("error", message);

        return ResponseEntity
                .status(status)
                .body(error);
    }

    // =========================
    // DETERMINE HTTP STATUS
    // =========================

    private HttpStatus determineStatus(String message) {

        String text = message.toLowerCase();

        // 404 - Resource not found
        if (text.contains("not found")) {
            return HttpStatus.NOT_FOUND;
        }

        // 403 - Ownership / permission errors
        if (text.contains("only admin")
                || text.contains("only update your own")
                || text.contains("only delete your own")
                || text.contains("only cancel your own")
                || text.contains("only view your own")
                || text.contains("only create a ticket for your own")
                || text.contains("only update your own notification")
                || text.contains("only delete your own notification")) {

            return HttpStatus.FORBIDDEN;
        }

        // 401 - Authentication errors
        if (text.contains("invalid email or password")) {
            return HttpStatus.UNAUTHORIZED;
        }

        // 400 - Bad request / business validation
        return HttpStatus.BAD_REQUEST;
    }
}