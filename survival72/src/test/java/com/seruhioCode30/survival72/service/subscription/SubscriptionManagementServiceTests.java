package com.seruhioCode30.survival72.service.subscription;

import com.seruhioCode30.survival72.model.Subscriber;
import com.seruhioCode30.survival72.model.SubscriberPreference;
import com.seruhioCode30.survival72.model.SubscriberStatus;
import com.seruhioCode30.survival72.repository.SubscriberRepository;
import com.seruhioCode30.survival72.service.join.ManagementTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({
        SubscriptionManagementService.class,
        ManagementTokenService.class
})
@TestPropertySource(properties = {
        "spring.test.database.replace=NONE",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class SubscriptionManagementServiceTests {

    @Autowired
    private SubscriptionManagementService subscriptionManagementService;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private ManagementTokenService managementTokenService;

    @Test
    void validTokenResolvesActiveSubscriber() {
        TokenFixture fixture = persistSubscriber(
                "valid-token@example.com",
                SubscriberStatus.ACTIVE,
                "Sergio",
                "CR",
                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
        );

        SubscriptionManagementView view =
                subscriptionManagementService.getSubscription(fixture.rawToken());

        assertThat(view.firstName()).isEqualTo("Sergio");
        assertThat(view.countryCode()).isEqualTo("CR");
    }

    @Test
    void invalidTokenFailsConsistently() {
        persistSubscriber(
                "invalid-token@example.com",
                SubscriberStatus.ACTIVE,
                "Sergio",
                "CR",
                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
        );

        assertThatThrownBy(() ->
                subscriptionManagementService.getSubscription("incorrect-token"))
                .isInstanceOf(SubscriptionAccessException.class)
                .hasMessage("Subscription access is not available");
    }

    @Test
    void blankOrRevokedTokenFailsConsistently() {
        assertThatThrownBy(() ->
                subscriptionManagementService.getSubscription("   "))
                .isInstanceOf(SubscriptionAccessException.class)
                .hasMessage("Subscription access is not available");

        Subscriber revoked = createSubscriber(
                "revoked@example.com",
                SubscriberStatus.UNSUBSCRIBED,
                "Sergio",
                "CR",
                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
        );
        revoked.setManagementTokenHash(null);
        subscriberRepository.saveAndFlush(revoked);

        assertThatThrownBy(() ->
                subscriptionManagementService.getSubscription("revoked-token"))
                .isInstanceOf(SubscriptionAccessException.class)
                .hasMessage("Subscription access is not available");
    }

    @Test
    void rawTokenIsHashedBeforeRepositoryLookup() {
        TokenFixture fixture = persistSubscriber(
                "hashed-lookup@example.com",
                SubscriberStatus.ACTIVE,
                "Sergio",
                "CR",
                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
        );

        assertThat(fixture.subscriber().getManagementTokenHash())
                .isEqualTo(managementTokenService.hashToken(fixture.rawToken()))
                .isNotEqualTo(fixture.rawToken());

        assertThat(subscriberRepository.findByManagementTokenHash(fixture.rawToken()))
                .isEmpty();

        assertThat(subscriptionManagementService.getSubscription(fixture.rawToken()))
                .isNotNull();
    }

    @Test
    void managementViewDoesNotExposeInternalIdOrTokenHash() {
        Set<String> componentNames = Arrays.stream(
                        SubscriptionManagementView.class.getRecordComponents()
                )
                .map(RecordComponent::getName)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(componentNames)
                .containsExactlyInAnyOrder(
                        "firstName",
                        "countryCode",
                        "preferences"
                )
                .doesNotContain(
                        "id",
                        "managementTokenHash",
                        "status",
                        "subscribedAt",
                        "updatedAt",
                        "unsubscribedAt",
                        "email"
                );
    }

    @Test
    void managementViewReturnsAllowedProfileFields() {
        TokenFixture fixture = persistSubscriber(
                "profile-view@example.com",
                SubscriberStatus.ACTIVE,
                "Sergio",
                "CR",
                Set.of(
                        SubscriberPreference.GENERAL_PREPAREDNESS,
                        SubscriberPreference.EMERGENCY_KIT
                )
        );

        SubscriptionManagementView view =
                subscriptionManagementService.getSubscription(fixture.rawToken());

        assertThat(view.firstName()).isEqualTo("Sergio");
        assertThat(view.countryCode()).isEqualTo("CR");
        assertThat(view.preferences())
                .containsExactlyInAnyOrder(
                        SubscriberPreference.GENERAL_PREPAREDNESS,
                        SubscriberPreference.EMERGENCY_KIT
                );
    }

    @Test
    void updateModifiesFirstName() {
        TokenFixture fixture = persistSubscriber(
                "update-name@example.com",
                SubscriberStatus.ACTIVE,
                "Old Name",
                "CR",
                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
        );

        subscriptionManagementService.updateSubscription(
                fixture.rawToken(),
                new UpdateSubscriptionCommand(
                        " New Name ",
                        "CR",
                        Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
                )
        );

        Subscriber reloaded = subscriberRepository
                .findByEmail("update-name@example.com")
                .orElseThrow();

        assertThat(reloaded.getFirstName()).isEqualTo("New Name");
    }

    @Test
    void updateNormalizesBlankFirstNameToNull() {
        TokenFixture fixture = persistSubscriber(
                "blank-name@example.com",
                SubscriberStatus.ACTIVE,
                "Old Name",
                "CR",
                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
        );

        subscriptionManagementService.updateSubscription(
                fixture.rawToken(),
                new UpdateSubscriptionCommand(
                        "   ",
                        "CR",
                        Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
                )
        );

        Subscriber reloaded = subscriberRepository
                .findByEmail("blank-name@example.com")
                .orElseThrow();

        assertThat(reloaded.getFirstName()).isNull();
    }

    @Test
    void updateNormalizesCountryCodeToUppercase() {
        TokenFixture fixture = persistSubscriber(
                "country-update@example.com",
                SubscriberStatus.ACTIVE,
                "Sergio",
                "CR",
                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
        );

        subscriptionManagementService.updateSubscription(
                fixture.rawToken(),
                new UpdateSubscriptionCommand(
                        "Sergio",
                        " us ",
                        Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
                )
        );

        Subscriber reloaded = subscriberRepository
                .findByEmail("country-update@example.com")
                .orElseThrow();

        assertThat(reloaded.getCountryCode()).isEqualTo("US");
    }

    @Test
    void updateReplacesPreferences() {
        TokenFixture fixture = persistSubscriber(
                "preferences-update@example.com",
                SubscriberStatus.ACTIVE,
                "Sergio",
                "CR",
                Set.of(
                        SubscriberPreference.GENERAL_PREPAREDNESS,
                        SubscriberPreference.EMERGENCY_KIT
                )
        );

        subscriptionManagementService.updateSubscription(
                fixture.rawToken(),
                new UpdateSubscriptionCommand(
                        "Sergio",
                        "CR",
                        Set.of(
                                SubscriberPreference.PRACTICAL_SKILLS,
                                SubscriberPreference.EVENTS_AND_UPDATES
                        )
                )
        );

        Subscriber reloaded = subscriberRepository
                .findByEmail("preferences-update@example.com")
                .orElseThrow();

        assertThat(reloaded.getPreferences())
                .containsExactlyInAnyOrder(
                        SubscriberPreference.PRACTICAL_SKILLS,
                        SubscriberPreference.EVENTS_AND_UPDATES
                );
    }

    @Test
    void updateRequiresAtLeastOnePreference() {
        TokenFixture fixture = persistSubscriber(
                "empty-update@example.com",
                SubscriberStatus.ACTIVE,
                "Sergio",
                "CR",
                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
        );

        assertThatThrownBy(() ->
                subscriptionManagementService.updateSubscription(
                        fixture.rawToken(),
                        new UpdateSubscriptionCommand(
                                "Sergio",
                                "CR",
                                Set.of()
                        )
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("preferences must contain at least one value");
    }

    @Test
    void updateChangesUpdatedAt() throws InterruptedException {
        TokenFixture fixture = persistSubscriber(
                "timestamp-update@example.com",
                SubscriberStatus.ACTIVE,
                "Old",
                "CR",
                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
        );

        LocalDateTime originalUpdatedAt = fixture.subscriber().getUpdatedAt();

        Thread.sleep(2);

        subscriptionManagementService.updateSubscription(
                fixture.rawToken(),
                new UpdateSubscriptionCommand(
                        "New",
                        "CR",
                        Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
                )
        );

        Subscriber reloaded = subscriberRepository
                .findByEmail("timestamp-update@example.com")
                .orElseThrow();

        assertThat(reloaded.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    void updateDoesNotChangeEmailOrStatus() {
        TokenFixture fixture = persistSubscriber(
                "immutable-fields@example.com",
                SubscriberStatus.ACTIVE,
                "Sergio",
                "CR",
                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
        );

        subscriptionManagementService.updateSubscription(
                fixture.rawToken(),
                new UpdateSubscriptionCommand(
                        "Updated",
                        "US",
                        Set.of(SubscriberPreference.PRACTICAL_SKILLS)
                )
        );

        Subscriber reloaded = subscriberRepository
                .findByEmail("immutable-fields@example.com")
                .orElseThrow();

        assertThat(reloaded.getEmail()).isEqualTo("immutable-fields@example.com");
        assertThat(reloaded.getStatus()).isEqualTo(SubscriberStatus.ACTIVE);
    }

    @Test
    void updateDoesNotRotateManagementToken() {
        TokenFixture fixture = persistSubscriber(
                "stable-token@example.com",
                SubscriberStatus.ACTIVE,
                "Sergio",
                "CR",
                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
        );

        String originalTokenHash = fixture.subscriber().getManagementTokenHash();

        subscriptionManagementService.updateSubscription(
                fixture.rawToken(),
                new UpdateSubscriptionCommand(
                        "Updated",
                        "US",
                        Set.of(SubscriberPreference.PRACTICAL_SKILLS)
                )
        );

        Subscriber reloaded = subscriberRepository
                .findByEmail("stable-token@example.com")
                .orElseThrow();

        assertThat(reloaded.getManagementTokenHash()).isEqualTo(originalTokenHash);
        assertThat(subscriptionManagementService.getSubscription(fixture.rawToken()))
                .isNotNull();
    }

    @Test
    void unsubscribedSubscriberCannotBeManagedEvenWithMatchingHash() {
        TokenFixture fixture = persistSubscriber(
                "unsubscribed-management@example.com",
                SubscriberStatus.UNSUBSCRIBED,
                "Sergio",
                "CR",
                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
        );

        assertThatThrownBy(() ->
                subscriptionManagementService.getSubscription(fixture.rawToken()))
                .isInstanceOf(SubscriptionAccessException.class)
                .hasMessage("Subscription access is not available");
    }

    @Test
    void updateRejectsInvalidCountryCode() {
        TokenFixture fixture = persistSubscriber(
                "invalid-country-update@example.com",
                SubscriberStatus.ACTIVE,
                "Sergio",
                "CR",
                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
        );

        assertThatThrownBy(() ->
                subscriptionManagementService.updateSubscription(
                        fixture.rawToken(),
                        new UpdateSubscriptionCommand(
                                "Sergio",
                                "CRI",
                                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
                        )
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "countryCode must contain exactly 2 alphabetic characters"
                );
    }

    private TokenFixture persistSubscriber(
            String email,
            SubscriberStatus status,
            String firstName,
            String countryCode,
            Set<SubscriberPreference> preferences
    ) {
        String rawToken = managementTokenService.generateToken();

        Subscriber subscriber = createSubscriber(
                email,
                status,
                firstName,
                countryCode,
                preferences
        );
        subscriber.setManagementTokenHash(
                managementTokenService.hashToken(rawToken)
        );

        Subscriber saved = subscriberRepository.saveAndFlush(subscriber);

        return new TokenFixture(saved, rawToken);
    }

    private Subscriber createSubscriber(
            String email,
            SubscriberStatus status,
            String firstName,
            String countryCode,
            Set<SubscriberPreference> preferences
    ) {
        LocalDateTime now = LocalDateTime.now().minusDays(1);

        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(email);
        subscriber.setFirstName(firstName);
        subscriber.setCountryCode(countryCode);
        subscriber.setStatus(status);
        subscriber.setSubscribedAt(now);
        subscriber.setUpdatedAt(now);
        subscriber.setUnsubscribedAt(
                status == SubscriberStatus.UNSUBSCRIBED
                        ? now.plusHours(1)
                        : null
        );
        subscriber.setPreferences(preferences);

        return subscriber;
    }

    private record TokenFixture(
            Subscriber subscriber,
            String rawToken
    ) {
    }
}
