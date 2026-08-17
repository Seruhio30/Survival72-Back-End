package com.seruhioCode30.survival72.controller.admin.newsletter.dto;

import com.seruhioCode30.survival72.model.SubscriberPreference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record AdminNewsletterCreateRequest(
        @NotBlank(message = "subject is required")
        @Size(max = 200, message = "subject must not exceed 200 characters")
        String subject,

        @NotBlank(message = "body is required")
        String body,

        @NotEmpty(message = "preferences are required")
        Set<SubscriberPreference> preferences
) {
}
