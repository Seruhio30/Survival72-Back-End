package com.seruhioCode30.survival72.repository;

import com.seruhioCode30.survival72.model.Subscriber;
import com.seruhioCode30.survival72.model.SubscriberPreference;
import com.seruhioCode30.survival72.model.SubscriberStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.test.database.replace=NONE",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class SubscriberRepositoryTests {

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Test
    void savesActiveSubscriberAndFindsByEmail() {
        Subscriber subscriber = createSubscriber("active@example.com");
        subscriber.setStatus(SubscriberStatus.ACTIVE);

        Subscriber saved = subscriberRepository.saveAndFlush(subscriber);

        assertThat(saved.getId()).isNotNull();
        assertThat(subscriberRepository.findByEmail("active@example.com"))
                .isPresent()
                .get()
                .extracting(Subscriber::getStatus)
                .isEqualTo(SubscriberStatus.ACTIVE);
    }

    @Test
    void persistsMultiplePreferences() {
        Subscriber subscriber = createSubscriber("preferences@example.com");
        subscriber.setPreferences(Set.of(
                SubscriberPreference.GENERAL_PREPAREDNESS,
                SubscriberPreference.EMERGENCY_KIT,
                SubscriberPreference.PRACTICAL_SKILLS
        ));

        Subscriber saved = subscriberRepository.saveAndFlush(subscriber);

        Subscriber reloaded = subscriberRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getPreferences())
                .containsExactlyInAnyOrder(
                        SubscriberPreference.GENERAL_PREPAREDNESS,
                        SubscriberPreference.EMERGENCY_KIT,
                        SubscriberPreference.PRACTICAL_SKILLS
                );
    }

    @Test
    void persistsUnsubscribedStatus() {
        Subscriber subscriber = createSubscriber("unsubscribed@example.com");
        subscriber.setStatus(SubscriberStatus.UNSUBSCRIBED);
        subscriber.setUnsubscribedAt(LocalDateTime.now());

        Subscriber saved = subscriberRepository.saveAndFlush(subscriber);

        assertThat(saved.getStatus()).isEqualTo(SubscriberStatus.UNSUBSCRIBED);
        assertThat(saved.getUnsubscribedAt()).isNotNull();
    }

    @Test
    void allowsNullableManagementTokenHashAndUnsubscribedAt() {
        Subscriber subscriber = createSubscriber("nullable@example.com");
        subscriber.setManagementTokenHash(null);
        subscriber.setUnsubscribedAt(null);

        Subscriber saved = subscriberRepository.saveAndFlush(subscriber);

        assertThat(saved.getManagementTokenHash()).isNull();
        assertThat(saved.getUnsubscribedAt()).isNull();
    }

    @Test
    void findsSubscriberByManagementTokenHash() {
        Subscriber subscriber = createSubscriber("management-token@example.com");
        subscriber.setManagementTokenHash(
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        );

        subscriberRepository.saveAndFlush(subscriber);

        assertThat(subscriberRepository.findByManagementTokenHash(
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        ))
                .isPresent()
                .get()
                .extracting(Subscriber::getEmail)
                .isEqualTo("management-token@example.com");
    }

    @Test
    void rejectsDuplicateEmail() {
        subscriberRepository.saveAndFlush(createSubscriber("duplicate@example.com"));

        Subscriber duplicate = createSubscriber("duplicate@example.com");

        assertThatThrownBy(() -> subscriberRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Subscriber createSubscriber(String email) {
        LocalDateTime now = LocalDateTime.now();

        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(email);
        subscriber.setFirstName("Test");
        subscriber.setCountryCode("CR");
        subscriber.setStatus(SubscriberStatus.ACTIVE);
        subscriber.setSubscribedAt(now);
        subscriber.setUpdatedAt(now);

        return subscriber;
    }

    @Test
    void findsSubscribersByStatusWithPagination() {
        Subscriber active = createSubscriber("status-active@example.com");
        active.setStatus(SubscriberStatus.ACTIVE);

        Subscriber unsubscribed = createSubscriber("status-unsubscribed@example.com");
        unsubscribed.setStatus(SubscriberStatus.UNSUBSCRIBED);
        unsubscribed.setUnsubscribedAt(LocalDateTime.now());

        subscriberRepository.saveAllAndFlush(Set.of(active, unsubscribed));

        var page = subscriberRepository.findByStatus(
                SubscriberStatus.ACTIVE,
                org.springframework.data.domain.PageRequest.of(
                        0,
                        20,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Order.desc("subscribedAt"),
                                org.springframework.data.domain.Sort.Order.desc("id")
                        )
                )
        );

        assertThat(page.getContent())
                .extracting(Subscriber::getEmail)
                .contains("status-active@example.com")
                .doesNotContain("status-unsubscribed@example.com");
    }

    @Test
    void findsSubscribersByPreferenceWithoutDuplicates() {
        Subscriber subscriber = createSubscriber("preference-filter@example.com");
        subscriber.setPreferences(Set.of(
                SubscriberPreference.EMERGENCY_KIT,
                SubscriberPreference.PRACTICAL_SKILLS
        ));

        subscriberRepository.saveAndFlush(subscriber);

        var page = subscriberRepository.findByPreference(
                SubscriberPreference.EMERGENCY_KIT,
                org.springframework.data.domain.PageRequest.of(0, 20)
        );

        assertThat(page.getContent())
                .filteredOn(item -> item.getEmail().equals("preference-filter@example.com"))
                .hasSize(1);
    }

    @Test
    void findsSubscribersByStatusAndPreference() {
        Subscriber matching = createSubscriber("combined-match@example.com");
        matching.setStatus(SubscriberStatus.ACTIVE);
        matching.setPreferences(Set.of(SubscriberPreference.PRACTICAL_SKILLS));

        Subscriber wrongStatus = createSubscriber("combined-wrong-status@example.com");
        wrongStatus.setStatus(SubscriberStatus.UNSUBSCRIBED);
        wrongStatus.setUnsubscribedAt(LocalDateTime.now());
        wrongStatus.setPreferences(Set.of(SubscriberPreference.PRACTICAL_SKILLS));

        Subscriber wrongPreference = createSubscriber("combined-wrong-preference@example.com");
        wrongPreference.setStatus(SubscriberStatus.ACTIVE);
        wrongPreference.setPreferences(Set.of(SubscriberPreference.EMERGENCY_KIT));

        subscriberRepository.saveAllAndFlush(
                Set.of(matching, wrongStatus, wrongPreference)
        );

        var page = subscriberRepository.findByStatusAndPreference(
                SubscriberStatus.ACTIVE,
                SubscriberPreference.PRACTICAL_SKILLS,
                org.springframework.data.domain.PageRequest.of(0, 20)
        );

        assertThat(page.getContent())
                .extracting(Subscriber::getEmail)
                .contains("combined-match@example.com")
                .doesNotContain(
                        "combined-wrong-status@example.com",
                        "combined-wrong-preference@example.com"
                );
    }

    @Test
    void usesStableSubscribedAtAndIdDescendingOrder() {
        LocalDateTime older = LocalDateTime.of(2026, 8, 15, 10, 0);
        LocalDateTime newer = LocalDateTime.of(2026, 8, 16, 10, 0);

        Subscriber olderSubscriber = createSubscriber("order-older@example.com");
        olderSubscriber.setSubscribedAt(older);
        olderSubscriber.setUpdatedAt(older);

        Subscriber newerSubscriber = createSubscriber("order-newer@example.com");
        newerSubscriber.setSubscribedAt(newer);
        newerSubscriber.setUpdatedAt(newer);

        subscriberRepository.saveAndFlush(olderSubscriber);
        subscriberRepository.saveAndFlush(newerSubscriber);

        var page = subscriberRepository.findAll(
                org.springframework.data.domain.PageRequest.of(
                        0,
                        20,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Order.desc("subscribedAt"),
                                org.springframework.data.domain.Sort.Order.desc("id")
                        )
                )
        );

        int newerIndex = page.getContent().stream()
                .map(Subscriber::getEmail)
                .toList()
                .indexOf("order-newer@example.com");

        int olderIndex = page.getContent().stream()
                .map(Subscriber::getEmail)
                .toList()
                .indexOf("order-older@example.com");

        assertThat(newerIndex).isLessThan(olderIndex);
    }

    @Test
    void findsActiveSubscribersMatchingAnySelectedPreferenceWithoutDuplicates() {
        Subscriber both = createSubscriber("newsletter-both@example.com");
        both.setStatus(SubscriberStatus.ACTIVE);
        both.setPreferences(Set.of(
                SubscriberPreference.EMERGENCY_KIT,
                SubscriberPreference.PRACTICAL_SKILLS
        ));

        Subscriber practicalOnly = createSubscriber("newsletter-practical@example.com");
        practicalOnly.setStatus(SubscriberStatus.ACTIVE);
        practicalOnly.setPreferences(Set.of(
                SubscriberPreference.PRACTICAL_SKILLS
        ));

        Subscriber eventsOnly = createSubscriber("newsletter-events@example.com");
        eventsOnly.setStatus(SubscriberStatus.ACTIVE);
        eventsOnly.setPreferences(Set.of(
                SubscriberPreference.EVENTS_AND_UPDATES
        ));

        Subscriber unsubscribedMatch =
                createSubscriber("newsletter-unsubscribed@example.com");
        unsubscribedMatch.setStatus(SubscriberStatus.UNSUBSCRIBED);
        unsubscribedMatch.setUnsubscribedAt(LocalDateTime.now());
        unsubscribedMatch.setPreferences(Set.of(
                SubscriberPreference.EMERGENCY_KIT,
                SubscriberPreference.PRACTICAL_SKILLS
        ));

        subscriberRepository.saveAllAndFlush(Set.of(
                both,
                practicalOnly,
                eventsOnly,
                unsubscribedMatch
        ));

        var page = subscriberRepository.findDistinctByStatusAndPreferencesIn(
                SubscriberStatus.ACTIVE,
                Set.of(
                        SubscriberPreference.EMERGENCY_KIT,
                        SubscriberPreference.PRACTICAL_SKILLS
                ),
                org.springframework.data.domain.PageRequest.of(0, 20)
        );

        assertThat(page.getContent())
                .extracting(Subscriber::getEmail)
                .contains(
                        "newsletter-both@example.com",
                        "newsletter-practical@example.com"
                )
                .doesNotContain(
                        "newsletter-events@example.com",
                        "newsletter-unsubscribed@example.com"
                );

        assertThat(page.getContent())
                .filteredOn(item ->
                        item.getEmail().equals("newsletter-both@example.com"))
                .hasSize(1);

        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void audienceQuerySupportsEventsAndUpdatesPreference() {
        Subscriber subscriber = createSubscriber("newsletter-events-match@example.com");
        subscriber.setStatus(SubscriberStatus.ACTIVE);
        subscriber.setPreferences(Set.of(
                SubscriberPreference.EVENTS_AND_UPDATES
        ));

        subscriberRepository.saveAndFlush(subscriber);

        var page = subscriberRepository.findDistinctByStatusAndPreferencesIn(
                SubscriberStatus.ACTIVE,
                Set.of(SubscriberPreference.EVENTS_AND_UPDATES),
                org.springframework.data.domain.PageRequest.of(0, 20)
        );

        assertThat(page.getContent())
                .extracting(Subscriber::getEmail)
                .contains("newsletter-events-match@example.com");
    }

}
