package com.fxai.auth;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(basePackages = {"com.fxai.auth", "com.fxai.user"})
public class AuthExceptionHandler {
    public record Error(String code, String message) {}

    @ExceptionHandler({AuthFailure.class, DataIntegrityViolationException.class})
    ResponseEntity<Error> authenticationFailure() {
        return ResponseEntity.status(401).cacheControl(CacheControl.noStore())
                .body(new Error("AUTHENTICATION_FAILED", "Authentication failed"));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class, IllegalArgumentException.class})
    ResponseEntity<Error> invalidRequest() {
        return ResponseEntity.badRequest().body(new Error("INVALID_REQUEST", "Invalid request"));
    }
}
