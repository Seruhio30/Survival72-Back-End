package com.seruhioCode30.survival72.service.join;

import com.seruhioCode30.survival72.model.Subscriber;
import com.seruhioCode30.survival72.model.SubscriberPreference;
import com.seruhioCode30.survival72.model.SubscriberStatus;
import com.seruhioCode30.survival72.repository.SubscriberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class JoinService {

    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MAX_FIRST_NAME_LENGTH = 80;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern COUNTRY_CODE_PATTERN = Pattern.compile("^[A-Z]{2}$");

    private final SubscriberRepository subscriberRepository;
    private final ManagementTokenService managementTokenService;

    public JoinService(
            SubscriberRepository subscriberRepository,
            ManagementTokenService managementTokenService
    ) {
        this.subscriberRepository = subscriberRepository;
        this.managementTokenService = managementTokenService;
    }

    @Transactional
    public JoinResult join(JoinCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("join command must not be null");
        }

        String email = normalizeEmail(command.email());
        String firstName = normalizeFirstName(command.firstName());
        String countryCode = normalizeCountryCode(command.countryCode());
        Set<SubscriberPreference> preferences = normalizePreferences(command.preferences());

        return subscriberRepository.findByEmail(email)
                .map(existing -> handleExistingSubscriber(
                        existing,
                        firstName,
                        countryCode,
                        preferences
                ))
                .orElseGet(() -> createNewSubscriber(
                        email,
                        firstName,
                        countryCode,
                        preferences
                ));
    }

    private JoinResult createNewSubscriber(
            String email,
            String firstName,
            String countryCode,
            Set<SubscriberPreference> preferences
    ) {
        LocalDateTime now = LocalDateTime.now();
        String rawToken = managementTokenService.generateToken();

        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(email);
        subscriber.setFirstName(firstName);
        subscriber.setCountryCode(countryCode);
        subscriber.setStatus(SubscriberStatus.ACTIVE);
        subscriber.setSubscribedAt(now);
        subscriber.setUpdatedAt(now);
        subscriber.setUnsubscribedAt(null);
        subscriber.setPreferences(preferences);
        subscriber.setManagementTokenHash(
                managementTokenService.hashToken(rawToken)
        );

        subscriberRepository.save(subscriber);

        return new JoinResult(
                JoinOutcome.NEW_SUBSCRIPTION,
                rawToken
        );
    }

    private JoinResult handleExistingSubscriber(
            Subscriber subscriber,
            String firstName,
            String countryCode,
            Set<SubscriberPreference> preferences
    ) {
        if (subscriber.getStatus() == SubscriberStatus.ACTIVE) {
            return new JoinResult(
                    JoinOutcome.ACTIVE_DUPLICATE,
                    null
            );
        }

        if (subscriber.getStatus() == SubscriberStatus.UNSUBSCRIBED) {
            return rejoin(
                    subscriber,
                    firstName,
                    countryCode,
                    preferences
            );
        }

        throw new IllegalStateException(
                "Unsupported subscriber status"
        );
    }

    private JoinResult rejoin(
            Subscriber subscriber,
            String firstName,
            String countryCode,
            Set<SubscriberPreference> preferences
    ) {
        LocalDateTime now = LocalDateTime.now();
        String rawToken = managementTokenService.generateToken();

        subscriber.setStatus(SubscriberStatus.ACTIVE);
        subscriber.setFirstName(firstName);
        subscriber.setCountryCode(countryCode);
        subscriber.setPreferences(preferences);
        subscriber.setSubscribedAt(now);
        subscriber.setUpdatedAt(now);
        subscriber.setUnsubscribedAt(null);
        subscriber.setManagementTokenHash(
                managementTokenService.hashToken(rawToken)
        );

        subscriberRepository.save(subscriber);

        return new JoinResult(
                JoinOutcome.REJOINED,
                rawToken
        );
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("email is required");
        }

        String normalized = email.trim().toLowerCase(Locale.ROOT);

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("email is required");
        }

        if (normalized.length() > MAX_EMAIL_LENGTH) {
            throw new IllegalArgumentException(
                    "email must not exceed 254 characters"
            );
        }

        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("email format is invalid");
        }

        return normalized;
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

        if (preferences.contains(null)) {
            throw new IllegalArgumentException(
                    "preferences must not contain null values"
            );
        }

        return new LinkedHashSet<>(preferences);
    }
}
