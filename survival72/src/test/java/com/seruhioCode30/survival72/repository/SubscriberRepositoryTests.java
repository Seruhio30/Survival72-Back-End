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
                SubscriberPreference.EDUCATIONAL_CONTENT
        ));

        Subscriber saved = subscriberRepository.saveAndFlush(subscriber);

        Subscriber reloaded = subscriberRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getPreferences())
                .containsExactlyInAnyOrder(
                        SubscriberPreference.GENERAL_PREPAREDNESS,
                        SubscriberPreference.EMERGENCY_KIT,
                        SubscriberPreference.EDUCATIONAL_CONTENT
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
}
