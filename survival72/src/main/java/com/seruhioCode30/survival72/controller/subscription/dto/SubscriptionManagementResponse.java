package com.seruhioCode30.survival72.controller.subscription.dto;

import com.seruhioCode30.survival72.model.SubscriberPreference;

import java.util.Set;

public record SubscriptionManagementResponse(
        String firstName,
        String countryCode,
        Set<SubscriberPreference> preferences
) {
}
