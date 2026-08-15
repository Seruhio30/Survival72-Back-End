package com.seruhioCode30.survival72.controller.subscription.dto;

import com.seruhioCode30.survival72.model.SubscriberPreference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record SubscriptionManagementRequest(
        @Size(max = 80, message = "firstName must not exceed 80 characters")
        String firstName,

        @NotBlank(message = "countryCode is required")
        @Pattern(
                regexp = "^[A-Za-z]{2}$",
                message = "countryCode must contain exactly 2 alphabetic characters"
        )
        String countryCode,

        @NotEmpty(message = "preferences must contain at least one value")
        Set<SubscriberPreference> preferences
) {
}
