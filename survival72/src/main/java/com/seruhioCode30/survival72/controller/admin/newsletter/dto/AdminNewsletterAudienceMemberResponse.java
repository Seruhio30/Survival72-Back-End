package com.seruhioCode30.survival72.controller.admin.newsletter.dto;

import com.seruhioCode30.survival72.model.SubscriberPreference;

import java.util.Set;

public record AdminNewsletterAudienceMemberResponse(
        Long id,
        String email,
        String firstName,
        Set<SubscriberPreference> preferences
) {
}
