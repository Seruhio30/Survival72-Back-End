package com.seruhioCode30.survival72.service.subscription;

import com.seruhioCode30.survival72.model.SubscriberPreference;

import java.util.Set;

public record UpdateSubscriptionCommand(
        String firstName,
        String countryCode,
        Set<SubscriberPreference> preferences
) {
}
