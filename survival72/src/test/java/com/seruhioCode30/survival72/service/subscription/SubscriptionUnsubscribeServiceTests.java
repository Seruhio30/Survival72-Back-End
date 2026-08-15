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

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({
        SubscriptionUnsubscribeService.class,
        SubscriptionManagementService.class,
        ManagementTokenService.class
})
@TestPropertySource(properties = {
        "spring.test.database.replace=NONE",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class SubscriptionUnsubscribeServiceTests {

    @Autowired
    private SubscriptionUnsubscribeService subscriptionUnsubscribeService;

    @Autowired
    private SubscriptionManagementService subscriptionManagementService;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private ManagementTokenService managementTokenService;

    @Test
    void validTokenUnsubscribesActiveSubscriber() {
        TokenFixture fixture = persistSubscriber(
                "unsubscribe@example.com",
                SubscriberStatus.ACTIVE
        );

        subscriptionUnsubscribeService.unsubscribe(fixture.rawToken());

        Subscriber reloaded = subscriberRepository
                .findById(fixture.subscriber().getId())
                .orElseThrow();

        assertThat(reloaded.getStatus())
                .isEqualTo(SubscriberStatus.UNSUBSCRIBED);
    }

    @Test
    void successfulUnsubscribeUpdatesLifecycleAndRevokesToken() {
        TokenFixture fixture = persistSubscriber(
                "lifecycle@example.com",
                SubscriberStatus.ACTIVE
        );

        LocalDateTime originalUpdatedAt = fixture.subscriber().getUpdatedAt();

        subscriptionUnsubscribeService.unsubscribe(fixture.rawToken());

        Subscriber reloaded = subscriberRepository
                .findById(fixture.subscriber().getId())
                .orElseThrow();

        assertThat(reloaded.getStatus())
                .isEqualTo(SubscriberStatus.UNSUBSCRIBED);
        assertThat(reloaded.getUnsubscribedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(reloaded.getManagementTokenHash()).isNull();
    }

    @Test
    void successfulUnsubscribePreservesSubscriberDataAndRow() {
        TokenFixture fixture = persistSubscriber(
                "preserved@example.com",
                SubscriberStatus.ACTIVE
        );

        Subscriber original = fixture.subscriber();

        Long originalId = original.getId();
        String originalEmail = original.getEmail();
        String originalFirstName = original.getFirstName();
        String originalCountryCode = original.getCountryCode();
        LocalDateTime originalSubscribedAt = original.getSubscribedAt();
        Set<SubscriberPreference> originalPreferences =
                Set.copyOf(original.getPreferences());
        long originalCount = subscriberRepository.count();

        subscriptionUnsubscribeService.unsubscribe(fixture.rawToken());

        subscriberRepository.flush();

        Subscriber reloaded = subscriberRepository
                .findById(originalId)
                .orElseThrow();

        assertThat(subscriberRepository.count()).isEqualTo(originalCount);
        assertThat(reloaded.getId()).isEqualTo(originalId);
        assertThat(reloaded.getEmail()).isEqualTo(originalEmail);
        assertThat(reloaded.getFirstName()).isEqualTo(originalFirstName);
        assertThat(reloaded.getCountryCode()).isEqualTo(originalCountryCode);
        assertThat(reloaded.getSubscribedAt()).isEqualTo(originalSubscribedAt);
        assertThat(reloaded.getPreferences())
                .containsExactlyInAnyOrderElementsOf(originalPreferences);
    }

    @Test
    void rawTokenIsHashedBeforeRepositoryLookup() {
        TokenFixture fixture = persistSubscriber(
                "hash-lookup@example.com",
                SubscriberStatus.ACTIVE
        );

        String expectedHash =
                managementTokenService.hashToken(fixture.rawToken());

        assertThat(fixture.subscriber().getManagementTokenHash())
                .isEqualTo(expectedHash)
                .isNotEqualTo(fixture.rawToken());

        assertThat(subscriberRepository
                .findByManagementTokenHash(fixture.rawToken()))
                .isEmpty();

        assertThat(subscriberRepository
                .findByManagementTokenHash(expectedHash))
                .isPresent();

        subscriptionUnsubscribeService.unsubscribe(fixture.rawToken());

        assertThat(subscriberRepository
                .findByManagementTokenHash(expectedHash))
                .isEmpty();
    }

    @Test
    void invalidTokenFailsNeutrally() {
        persistSubscriber(
                "invalid@example.com",
                SubscriberStatus.ACTIVE
        );

        assertNeutralFailure("not-a-valid-management-token");
    }

    @Test
    void blankTokenFailsNeutrally() {
        assertNeutralFailure("   ");
    }

    @Test
    void nullTokenFailsNeutrally() {
        assertNeutralFailure(null);
    }

    @Test
    void revokedTokenFailsNeutrally() {
        TokenFixture fixture = persistSubscriber(
                "revoked@example.com",
                SubscriberStatus.ACTIVE
        );

        fixture.subscriber().setManagementTokenHash(null);
        subscriberRepository.saveAndFlush(fixture.subscriber());

        assertNeutralFailure(fixture.rawToken());
    }

    @Test
    void unsubscribedSubscriberCannotBeProcessedAgainEvenWithMatchingHash() {
        TokenFixture fixture = persistSubscriber(
                "already-unsubscribed@example.com",
                SubscriberStatus.UNSUBSCRIBED
        );

        assertNeutralFailure(fixture.rawToken());
    }

    @Test
    void previousTokenStopsResolvingAfterSuccessfulUnsubscribe() {
        TokenFixture fixture = persistSubscriber(
                "old-token@example.com",
                SubscriberStatus.ACTIVE
        );

        String originalHash =
                fixture.subscriber().getManagementTokenHash();

        subscriptionUnsubscribeService.unsubscribe(fixture.rawToken());

        assertThat(subscriberRepository
                .findByManagementTokenHash(originalHash))
                .isEmpty();

        assertThatThrownBy(() ->
                subscriptionManagementService
                        .getSubscription(fixture.rawToken()))
                .isInstanceOf(SubscriptionAccessException.class)
                .hasMessage("Subscription access is not available");

        assertNeutralFailure(fixture.rawToken());
    }

    @Test
    void unsubscribeDoesNotCreateDuplicateSubscribers() {
        TokenFixture fixture = persistSubscriber(
                "no-duplicate@example.com",
                SubscriberStatus.ACTIVE
        );

        long before = subscriberRepository.count();

        subscriptionUnsubscribeService.unsubscribe(fixture.rawToken());

        subscriberRepository.flush();

        assertThat(subscriberRepository.count()).isEqualTo(before);

        assertThat(subscriberRepository
                .findByEmail("no-duplicate@example.com"))
                .isPresent();
    }

    private void assertNeutralFailure(String rawToken) {
        assertThatThrownBy(() ->
                subscriptionUnsubscribeService.unsubscribe(rawToken))
                .isInstanceOf(SubscriptionAccessException.class)
                .hasMessage("Subscription access is not available");
    }

    private TokenFixture persistSubscriber(
            String email,
            SubscriberStatus status
    ) {
        LocalDateTime subscribedAt =
                LocalDateTime.now().minusDays(3);
        LocalDateTime updatedAt =
                LocalDateTime.now().minusDays(2);

        String rawToken = managementTokenService.generateToken();

        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(email);
        subscriber.setFirstName("Sergio");
        subscriber.setCountryCode("CR");
        subscriber.setStatus(status);
        subscriber.setSubscribedAt(subscribedAt);
        subscriber.setUpdatedAt(updatedAt);
        subscriber.setUnsubscribedAt(
                status == SubscriberStatus.UNSUBSCRIBED
                        ? LocalDateTime.now().minusDays(1)
                        : null
        );
        subscriber.setManagementTokenHash(
                managementTokenService.hashToken(rawToken)
        );
        subscriber.setPreferences(Set.of(
                SubscriberPreference.GENERAL_PREPAREDNESS,
                SubscriberPreference.EMERGENCY_KIT
        ));

        Subscriber saved =
                subscriberRepository.saveAndFlush(subscriber);

        return new TokenFixture(saved, rawToken);
    }

    private record TokenFixture(
            Subscriber subscriber,
            String rawToken
    ) {
    }
}
