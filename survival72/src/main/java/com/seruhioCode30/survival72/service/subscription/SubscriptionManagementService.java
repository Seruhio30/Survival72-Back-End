package com.seruhioCode30.survival72.service.subscription;

import com.seruhioCode30.survival72.model.Subscriber;
import com.seruhioCode30.survival72.model.SubscriberPreference;
import com.seruhioCode30.survival72.model.SubscriberStatus;
import com.seruhioCode30.survival72.repository.SubscriberRepository;
import com.seruhioCode30.survival72.service.join.ManagementTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class SubscriptionManagementService {

    private static final int MAX_FIRST_NAME_LENGTH = 80;
    private static final Pattern COUNTRY_CODE_PATTERN = Pattern.compile("^[A-Z]{2}$");

    private final SubscriberRepository subscriberRepository;
    private final ManagementTokenService managementTokenService;

    public SubscriptionManagementService(
            SubscriberRepository subscriberRepository,
            ManagementTokenService managementTokenService
    ) {
        this.subscriberRepository = subscriberRepository;
        this.managementTokenService = managementTokenService;
    }

    @Transactional(readOnly = true)
    public SubscriptionManagementView getSubscription(String rawToken) {
        Subscriber subscriber = resolveActiveSubscriber(rawToken);

        return toView(subscriber);
    }

    @Transactional
    public SubscriptionManagementView updateSubscription(
            String rawToken,
            UpdateSubscriptionCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException(
                    "update subscription command must not be null"
            );
        }

        Subscriber subscriber = resolveActiveSubscriber(rawToken);

        String firstName = normalizeFirstName(command.firstName());
        String countryCode = normalizeCountryCode(command.countryCode());
        Set<SubscriberPreference> preferences =
                normalizePreferences(command.preferences());

        subscriber.setFirstName(firstName);
        subscriber.setCountryCode(countryCode);
        subscriber.setPreferences(preferences);
        subscriber.setUpdatedAt(LocalDateTime.now());

        subscriberRepository.save(subscriber);

        return toView(subscriber);
    }

    private Subscriber resolveActiveSubscriber(String rawToken) {
        String tokenHash;

        try {
            tokenHash = managementTokenService.hashToken(rawToken);
        } catch (IllegalArgumentException exception) {
            throw new SubscriptionAccessException();
        }

        Subscriber subscriber = subscriberRepository
                .findByManagementTokenHash(tokenHash)
                .orElseThrow(SubscriptionAccessException::new);

        if (subscriber.getStatus() != SubscriberStatus.ACTIVE) {
            throw new SubscriptionAccessException();
        }

        if (subscriber.getManagementTokenHash() == null) {
            throw new SubscriptionAccessException();
        }

        return subscriber;
    }

    private SubscriptionManagementView toView(Subscriber subscriber) {
        return new SubscriptionManagementView(
                subscriber.getFirstName(),
                subscriber.getCountryCode(),
                new LinkedHashSet<>(subscriber.getPreferences())
        );
    }

    private String normalizeFirstName(String firstName) {
        if (firstName == null) {
            return null;
        }

        String normalized = firstName.trim();

        if (normalized.isEmpty()) {
            return null;
        }

        if (normalized.length() > MAX_FIRST_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "firstName must not exceed 80 characters"
            );
        }

        return normalized;
    }

    private String normalizeCountryCode(String countryCode) {
        if (countryCode == null) {
            throw new IllegalArgumentException("countryCode is required");
        }

        String normalized = countryCode.trim().toUpperCase(Locale.ROOT);

        if (!COUNTRY_CODE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "countryCode must contain exactly 2 alphabetic characters"
            );
        }

        return normalized;
    }

    private Set<SubscriberPreference> normalizePreferences(
            Set<SubscriberPreference> preferences
    ) {
        if (preferences == null || preferences.isEmpty()) {
            throw new IllegalArgumentException(
                    "preferences must contain at least one value"
            );
        }

        if (preferences.stream().anyMatch(value -> value == null)) {
            throw new IllegalArgumentException(
                    "preferences must not contain null values"
            );
        }

        return new LinkedHashSet<>(preferences);
    }
}
