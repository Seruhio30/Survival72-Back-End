package com.seruhioCode30.survival72.service.join;

import java.util.Objects;
import java.util.Optional;

public record JoinResult(
        JoinOutcome outcome,
        String rawManagementToken
) {

    public JoinResult {
        Objects.requireNonNull(outcome, "outcome must not be null");

        if (outcome == JoinOutcome.ACTIVE_DUPLICATE && rawManagementToken != null) {
            throw new IllegalArgumentException(
                    "ACTIVE_DUPLICATE must not contain a management token"
            );
        }

        if (outcome != JoinOutcome.ACTIVE_DUPLICATE && rawManagementToken == null) {
            throw new IllegalArgumentException(
                    "A generated management token is required for new subscriptions and rejoins"
            );
        }
    }

    public Optional<String> managementToken() {
        return Optional.ofNullable(rawManagementToken);
    }
}
