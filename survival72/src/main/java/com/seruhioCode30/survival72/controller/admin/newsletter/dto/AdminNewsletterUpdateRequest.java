package com.seruhioCode30.survival72.controller.admin.newsletter.dto;

import com.seruhioCode30.survival72.model.SubscriberPreference;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record AdminNewsletterUpdateRequest(
        @Size(max = 200, message = "subject must not exceed 200 characters")
        String subject,

        String body,

        Set<SubscriberPreference> preferences
) {
}
