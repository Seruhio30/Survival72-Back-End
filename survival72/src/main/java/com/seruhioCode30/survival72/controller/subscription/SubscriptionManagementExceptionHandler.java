package com.seruhioCode30.survival72.controller.subscription;

import com.seruhioCode30.survival72.service.subscription.SubscriptionAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = SubscriptionManagementController.class)
public class SubscriptionManagementExceptionHandler {

    @ExceptionHandler(SubscriptionAccessException.class)
    public ResponseEntity<Map<String, String>> handleSubscriptionAccessError(
            SubscriptionAccessException exception
    ) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("code", "SUBSCRIPTION_ACCESS_NOT_FOUND");
        body.put(
                "message",
                "The subscription management link is invalid or no longer available."
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidUpdate(
            IllegalArgumentException exception
    ) {
        return badRequest();
    }

    private ResponseEntity<Map<String, String>> badRequest() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("code", "BAD_REQUEST");
        body.put("message", "Invalid subscription management request.");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body);
    }
}
