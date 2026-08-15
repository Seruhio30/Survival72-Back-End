package com.seruhioCode30.survival72.controller.subscription;

import com.seruhioCode30.survival72.service.subscription.SubscriptionAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = SubscriptionUnsubscribeController.class)
public class SubscriptionUnsubscribeExceptionHandler {

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
}
