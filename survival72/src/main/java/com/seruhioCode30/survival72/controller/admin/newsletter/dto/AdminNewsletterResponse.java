package com.seruhioCode30.survival72.controller.admin.newsletter.dto;

import com.seruhioCode30.survival72.model.NewsletterStatus;
import com.seruhioCode30.survival72.model.SubscriberPreference;

import java.time.LocalDateTime;
import java.util.Set;

public record AdminNewsletterResponse(
        Long id,
        String subject,
        String body,
        NewsletterStatus status,
        Set<SubscriberPreference> preferences,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime sentAt
) {
}
