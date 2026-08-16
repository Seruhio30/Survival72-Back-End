package com.seruhioCode30.survival72.controller.admin.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = AdminAuthController.class)
public class AdminAuthExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationFailure(
            AuthenticationException exception
    ) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("code", "ADMIN_AUTHENTICATION_FAILED");
        body.put("message", "Invalid administrator credentials.");

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationError(
            MethodArgumentNotValidException exception
    ) {
        return badRequest();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableRequest(
            HttpMessageNotReadableException exception
    ) {
        return badRequest();
    }

    private ResponseEntity<Map<String, String>> badRequest() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("code", "BAD_REQUEST");
        body.put("message", "Invalid administrator authentication request.");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body);
    }
}
