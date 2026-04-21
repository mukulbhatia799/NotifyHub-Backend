package com.notifyhub.exception;

import com.notifyhub.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex,
                                                         HttpServletRequest request) {
        log.debug("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler({ValidationException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorResponse> handleValidation(Exception ex,
                                                           HttpServletRequest request) {
        if (ex instanceof MethodArgumentNotValidException manve) {
            Map<String, String> fieldErrors = new HashMap<>();
            for (FieldError fe : manve.getBindingResult().getFieldErrors()) {
                fieldErrors.put(fe.getField(), fe.getDefaultMessage());
            }
            ErrorResponse body = ErrorResponse.builder()
                    .timestamp(java.time.Instant.now())
                    .status(400)
                    .error("Bad Request")
                    .message("Validation failed")
                    .path(request.getRequestURI())
                    .fieldErrors(fieldErrors)
                    .build();
            return ResponseEntity.badRequest().body(body);
        }

        ValidationException ve = (ValidationException) ex;
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(java.time.Instant.now())
                .status(400)
                .error("Bad Request")
                .message(ve.getMessage())
                .path(request.getRequestURI())
                .fieldErrors(ve.getFieldErrors().isEmpty() ? null : ve.getFieldErrors())
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler({AuthException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleAuth(Exception ex,
                                                     HttpServletRequest request) {
        log.debug("Auth error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(401, "Unauthorized", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException ex,
                                                          HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitException ex,
                                                          HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(ErrorResponse.of(429, "Too Many Requests", ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex,
                                                        HttpServletRequest request) {
        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Internal Server Error",
                        "An unexpected error occurred", request.getRequestURI()));
    }
}
