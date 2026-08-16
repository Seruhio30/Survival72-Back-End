package com.seruhioCode30.survival72.controller.subscription;

import com.seruhioCode30.survival72.controller.subscription.dto.SubscriptionUnsubscribeResponse;
import com.seruhioCode30.survival72.service.subscription.SubscriptionAccessException;
import com.seruhioCode30.survival72.service.subscription.SubscriptionUnsubscribeApplicationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions/unsubscribe")
public class SubscriptionUnsubscribeController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final SubscriptionUnsubscribeApplicationService unsubscribeApplicationService;

    public SubscriptionUnsubscribeController(
            SubscriptionUnsubscribeApplicationService unsubscribeApplicationService
    ) {
        this.unsubscribeApplicationService = unsubscribeApplicationService;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SubscriptionUnsubscribeResponse> unsubscribe(
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            ) String authorizationHeader
    ) {
        String rawToken = extractBearerToken(authorizationHeader);

        unsubscribeApplicationService.unsubscribe(rawToken);

        return ResponseEntity.ok(
                new SubscriptionUnsubscribeResponse(
                        "UNSUBSCRIBED",
                        "Subscription cancelled successfully."
                )
        );
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null
                || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new SubscriptionAccessException();
        }

        String rawToken = authorizationHeader
                .substring(BEARER_PREFIX.length())
                .trim();

        if (rawToken.isEmpty()) {
            throw new SubscriptionAccessException();
        }

        return rawToken;
    }
}
