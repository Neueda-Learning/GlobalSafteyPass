package com.globalsafetypass.api;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<Map<String, String>> badRequest(Exception error) {
        String message = error instanceof MethodArgumentNotValidException validation
            ? validation.getBindingResult().getFieldErrors().stream().findFirst()
                .map(e -> e.getField() + ": " + e.getDefaultMessage()).orElse("Invalid request.")
            : error.getMessage();
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> notFound(Exception error) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Resource not found."));
    }
}

