package com.travelassistant.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String,Object>> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage()).findFirst().orElse("Invalid request");
        return body(HttpStatus.BAD_REQUEST, message, req);
    }
    @ExceptionHandler({InvalidTripException.class, IllegalArgumentException.class})
    ResponseEntity<Map<String,Object>> bad(RuntimeException ex, HttpServletRequest req) { return body(HttpStatus.BAD_REQUEST, ex.getMessage(), req); }
    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<Map<String,Object>> unauthorized(RuntimeException ex, HttpServletRequest req) { return body(HttpStatus.UNAUTHORIZED, ex.getMessage(), req); }
    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<Map<String,Object>> forbidden(RuntimeException ex, HttpServletRequest req) { return body(HttpStatus.FORBIDDEN, ex.getMessage(), req); }
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<Map<String,Object>> missing(RuntimeException ex, HttpServletRequest req) { return body(HttpStatus.NOT_FOUND, ex.getMessage(), req); }
    @ExceptionHandler(DuplicateTransactionException.class)
    ResponseEntity<Map<String,Object>> conflict(RuntimeException ex, HttpServletRequest req) { return body(HttpStatus.CONFLICT, ex.getMessage(), req); }
    @ExceptionHandler(ExternalServiceException.class)
    ResponseEntity<Map<String,Object>> external(RuntimeException ex, HttpServletRequest req) { return body(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), req); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String,Object>> unexpected(Exception ex, HttpServletRequest req) { return body(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", req); }
    private ResponseEntity<Map<String,Object>> body(HttpStatus status, String message, HttpServletRequest req) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("timestamp", Instant.now()); m.put("status", status.value()); m.put("error", status.getReasonPhrase());
        m.put("message", message); m.put("path", req.getRequestURI());
        return ResponseEntity.status(status).body(m);
    }
}
