package com.seruhioCode30.survival72.service.join;

import com.seruhioCode30.survival72.model.Subscriber;
import com.seruhioCode30.survival72.model.SubscriberPreference;
import com.seruhioCode30.survival72.model.SubscriberStatus;
import com.seruhioCode30.survival72.repository.SubscriberRepository;
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
        JoinService.class,
        ManagementTokenService.class
})
@TestPropertySource(properties = {
        "spring.test.database.replace=NONE",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class JoinServiceTests {

    @Autowired
    private JoinService joinService;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private ManagementTokenService managementTokenService;

    @Test
    void newJoinNormalizesEmailAndCreatesActiveSubscriber() {
        JoinResult result = joinService.join(new JoinCommand(
                "  Sergio@Example.COM  ",
                " Sergio ",
                " cr ",
                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
        ));

        Subscriber saved = subscriberRepository
                .findByEmail("sergio@example.com")
                .orElseThrow();

        assertThat(result.outcome()).isEqualTo(JoinOutcome.NEW_SUBSCRIPTION);
        assertThat(saved.getEmail()).isEqualTo("sergio@example.com");
        assertThat(saved.getStatus()).isEqualTo(SubscriberStatus.ACTIVE);
        assertThat(saved.getFirstName()).isEqualTo("Sergio");
        assertThat(saved.getCountryCode()).isEqualTo("CR");
        assertThat(saved.getSubscribedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getUnsubscribedAt()).isNull();
    }

    @Test
    void newJoinPersistsRequestedPreferencesWithoutDuplicates() {
        joinService.join(new JoinCommand(
                "preferences-join@example.com",
                "Test",
                "CR",
                Set.of(
                        SubscriberPreference.GENERAL_PREPAREDNESS,
                        SubscriberPreference.EMERGENCY_KIT
                )
        ));

        Subscriber saved = subscriberRepository
                .findByEmail("preferences-join@example.com")
                .orElseThrow();

        assertThat(saved.getPreferences())
                .containsExactlyInAnyOrder(
                        SubscriberPreference.GENERAL_PREPAREDNESS,
                        SubscriberPreference.EMERGENCY_KIT
                );
    }

    @Test
    void newJoinStoresSha256HashAndReturnsRawTokenOnlyInResult() {
        JoinResult result = joinService.join(new JoinCommand(
                "token-new@example.com",
                "Test",
                "CR",
                Set.of(SubscriberPreference.PRACTICAL_SKILLS)
        ));

        Subscriber saved = subscriberRepository
                .findByEmail("token-new@example.com")
                .orElseThrow();

        String rawToken = result.managementToken().orElseThrow();

        assertThat(saved.getManagementTokenHash())
                .matches("^[0-9a-f]{64}$")
                .isNotEqualTo(rawToken);

        assertThat(managementTokenService.hashToken(rawToken))
                .isEqualTo(saved.getManagementTokenHash());
    }

    @Test
    void activeDuplicateDoesNotCreateOrModifySubscriber() {
        Subscriber original = persistSubscriber(
                "duplicate-active@example.com",
                SubscriberStatus.ACTIVE,
                "Original",
                "CR",
                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
        );

        Long originalId = original.getId();
        LocalDateTime originalSubscribedAt = original.getSubscribedAt();
        LocalDateTime originalUpdatedAt = original.getUpdatedAt();
        String originalTokenHash = original.getManagementTokenHash();

        JoinResult result = joinService.join(new JoinCommand(
                " DUPLICATE-ACTIVE@example.com ",
                "Changed",
                "US",
                Set.of(SubscriberPreference.EVENTS_AND_UPDATES)
        ));

        subscriberRepository.flush();
        Subscriber reloaded = subscriberRepository.findById(originalId).orElseThrow();

        assertThat(result.outcome()).isEqualTo(JoinOutcome.ACTIVE_DUPLICATE);
        assertThat(result.managementToken()).isEmpty();
        assertThat(
                subscriberRepository
                        .findByEmail("duplicate-active@example.com")
                        .orElseThrow()
                        .getId()
        ).isEqualTo(originalId);

        assertThat(reloaded.getFirstName()).isEqualTo("Original");
        assertThat(reloaded.getCountryCode()).isEqualTo("CR");
        assertThat(reloaded.getPreferences())
                .containsExactly(SubscriberPreference.GENERAL_PREPAREDNESS);
        assertThat(reloaded.getSubscribedAt()).isEqualTo(originalSubscribedAt);
        assertThat(reloaded.getUpdatedAt()).isEqualTo(originalUpdatedAt);
        assertThat(reloaded.getManagementTokenHash()).isEqualTo(originalTokenHash);
    }

    @Test
    void rejoinReusesSameSubscriberAndReactivatesLifecycle() {
        Subscriber original = persistSubscriber(
                "rejoin@example.com",
                SubscriberStatus.UNSUBSCRIBED,
                "Old Name",
                "CR",
                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
        );

        Long originalId = original.getId();
        LocalDateTime oldSubscribedAt = original.getSubscribedAt();
        String oldTokenHash = original.getManagementTokenHash();

        original.setUnsubscribedAt(LocalDateTime.now().minusDays(1));
        original.setUpdatedAt(LocalDateTime.now().minusDays(1));
        subscriberRepository.saveAndFlush(original);

        JoinResult result = joinService.join(new JoinCommand(
                " REJOIN@example.com ",
                " New Name ",
                " us ",
                Set.of(
                        SubscriberPreference.EMERGENCY_KIT,
                        SubscriberPreference.PRACTICAL_SKILLS
                )
        ));

        subscriberRepository.flush();
        Subscriber reloaded = subscriberRepository.findById(originalId).orElseThrow();
        String rawToken = result.managementToken().orElseThrow();

        assertThat(result.outcome()).isEqualTo(JoinOutcome.REJOINED);
        assertThat(reloaded.getId()).isEqualTo(originalId);
        assertThat(
                subscriberRepository
                        .findByEmail("rejoin@example.com")
                        .orElseThrow()
                        .getId()
        ).isEqualTo(originalId);

        assertThat(reloaded.getStatus()).isEqualTo(SubscriberStatus.ACTIVE);
        assertThat(reloaded.getFirstName()).isEqualTo("New Name");
        assertThat(reloaded.getCountryCode()).isEqualTo("US");
        assertThat(reloaded.getPreferences())
                .containsExactlyInAnyOrder(
                        SubscriberPreference.EMERGENCY_KIT,
                        SubscriberPreference.PRACTICAL_SKILLS
                );

        assertThat(reloaded.getSubscribedAt()).isAfter(oldSubscribedAt);
        assertThat(reloaded.getUpdatedAt()).isNotNull();
        assertThat(reloaded.getUnsubscribedAt()).isNull();

        assertThat(reloaded.getManagementTokenHash())
                .matches("^[0-9a-f]{64}$")
                .isNotEqualTo(oldTokenHash);

        assertThat(managementTokenService.hashToken(rawToken))
                .isEqualTo(reloaded.getManagementTokenHash());
    }

    @Test
    void blankFirstNameBecomesNullAndCountryCodeIsUppercase() {
        joinService.join(new JoinCommand(
                "normalization@example.com",
                "   ",
                " cr ",
                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
        ));

        Subscriber saved = subscriberRepository
                .findByEmail("normalization@example.com")
                .orElseThrow();

        assertThat(saved.getFirstName()).isNull();
        assertThat(saved.getCountryCode()).isEqualTo("CR");
    }

    @Test
    void rejectsMissingOrInvalidEmail() {
        assertThatThrownBy(() -> joinService.join(new JoinCommand(
                "not-an-email",
                null,
                "CR",
                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email format is invalid");

        assertThatThrownBy(() -> joinService.join(new JoinCommand(
                "   ",
                null,
                "CR",
                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email is required");
    }

    @Test
    void rejectsInvalidCountryCode() {
        assertThatThrownBy(() -> joinService.join(new JoinCommand(
                "country-invalid@example.com",
                null,
                "CRI",
                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("countryCode must contain exactly 2 alphabetic characters");

        assertThatThrownBy(() -> joinService.join(new JoinCommand(
                "country-number@example.com",
                null,
                "C1",
                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("countryCode must contain exactly 2 alphabetic characters");
    }

    @Test
    void rejectsEmptyOrNullPreferences() {
        assertThatThrownBy(() -> joinService.join(new JoinCommand(
                "empty-preferences@example.com",
                null,
                "CR",
                Set.of()
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("preferences must contain at least one value");

        assertThatThrownBy(() -> joinService.join(new JoinCommand(
                "null-preferences@example.com",
                null,
                "CR",
                null
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("preferences must contain at least one value");
    }

    @Test
    void rejectsFirstNameLongerThanContractLimit() {
        String longName = "a".repeat(81);

        assertThatThrownBy(() -> joinService.join(new JoinCommand(
                "long-name@example.com",
                longName,
                "CR",
                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("firstName must not exceed 80 characters");
    }

    private Subscriber persistSubscriber(
            String email,
            SubscriberStatus status,
            String firstName,
            String countryCode,
            Set<SubscriberPreference> preferences
    ) {
        LocalDateTime now = LocalDateTime.now().minusDays(2);

        String rawToken = managementTokenService.generateToken();

        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(email);
        subscriber.setFirstName(firstName);
        subscriber.setCountryCode(countryCode);
        subscriber.setStatus(status);
        subscriber.setSubscribedAt(now);
        subscriber.setUpdatedAt(now);
        subscriber.setUnsubscribedAt(
                status == SubscriberStatus.UNSUBSCRIBED
                        ? now.plusDays(1)
                        : null
        );
        subscriber.setManagementTokenHash(
                managementTokenService.hashToken(rawToken)
        );
        subscriber.setPreferences(preferences);

        return subscriberRepository.saveAndFlush(subscriber);
    }
}
