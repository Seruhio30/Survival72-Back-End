package com.seruhioCode30.survival72.controller.subscription;

import com.seruhioCode30.survival72.controller.subscription.dto.SubscriptionManagementRequest;
import com.seruhioCode30.survival72.controller.subscription.dto.SubscriptionManagementResponse;
import com.seruhioCode30.survival72.service.subscription.SubscriptionAccessException;
import com.seruhioCode30.survival72.service.subscription.SubscriptionManagementService;
import com.seruhioCode30.survival72.service.subscription.SubscriptionManagementView;
import com.seruhioCode30.survival72.service.subscription.UpdateSubscriptionCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions/manage")
public class SubscriptionManagementController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final SubscriptionManagementService subscriptionManagementService;

    public SubscriptionManagementController(
            SubscriptionManagementService subscriptionManagementService
    ) {
        this.subscriptionManagementService = subscriptionManagementService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SubscriptionManagementResponse> getSubscription(
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            ) String authorizationHeader
    ) {
        String rawToken = extractBearerToken(authorizationHeader);

        SubscriptionManagementView view =
                subscriptionManagementService.getSubscription(rawToken);

        return ResponseEntity.ok(toResponse(view));
    }

    @PatchMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<SubscriptionManagementResponse> updateSubscription(
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            ) String authorizationHeader,
            @Valid @RequestBody SubscriptionManagementRequest request
    ) {
        String rawToken = extractBearerToken(authorizationHeader);

        UpdateSubscriptionCommand command = new UpdateSubscriptionCommand(
                request.firstName(),
                request.countryCode(),
                request.preferences()
        );

        SubscriptionManagementView view =
                subscriptionManagementService.updateSubscription(
                        rawToken,
                        command
                );

        return ResponseEntity.ok(toResponse(view));
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

    private SubscriptionManagementResponse toResponse(
            SubscriptionManagementView view
    ) {
        return new SubscriptionManagementResponse(
                view.firstName(),
                view.countryCode(),
                view.preferences()
        );
    }
}
