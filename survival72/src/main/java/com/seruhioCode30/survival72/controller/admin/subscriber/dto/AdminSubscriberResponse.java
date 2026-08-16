package com.seruhioCode30.survival72.controller.admin.subscriber.dto;

import com.seruhioCode30.survival72.model.SubscriberPreference;
import com.seruhioCode30.survival72.model.SubscriberStatus;

import java.time.LocalDateTime;
import java.util.Set;

public record AdminSubscriberResponse(
        Long id,
        String email,
        String firstName,
        String countryCode,
        SubscriberStatus status,
        Set<SubscriberPreference> preferences,
        LocalDateTime subscribedAt,
        LocalDateTime updatedAt,
        LocalDateTime unsubscribedAt
) {
}
