package com.seruhioCode30.survival72.service.join;

import com.seruhioCode30.survival72.model.SubscriberPreference;

import java.util.LinkedHashSet;
import java.util.Set;

public record JoinCommand(
        String email,
        String firstName,
        String countryCode,
        Set<SubscriberPreference> preferences
) {

    public JoinCommand {
        preferences = preferences == null
                ? null
                : new LinkedHashSet<>(preferences);
    }

    @Override
    public Set<SubscriberPreference> preferences() {
        return preferences == null
                ? null
                : new LinkedHashSet<>(preferences);
    }
}
