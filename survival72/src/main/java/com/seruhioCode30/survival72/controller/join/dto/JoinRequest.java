package com.seruhioCode30.survival72.controller.join.dto;

import com.seruhioCode30.survival72.model.SubscriberPreference;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record JoinRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email format is invalid")
        @Size(max = 254, message = "email must not exceed 254 characters")
        String email,

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
