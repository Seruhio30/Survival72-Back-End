package com.seruhioCode30.survival72.repository;

import com.seruhioCode30.survival72.model.Newsletter;
import com.seruhioCode30.survival72.model.NewsletterStatus;
import com.seruhioCode30.survival72.model.SubscriberPreference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.test.database.replace=NONE",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class NewsletterRepositoryTests {

    @Autowired
    private NewsletterRepository newsletterRepository;

    @Test
    void persistsNewsletterWithCanonicalPreferences() {
        LocalDateTime now = LocalDateTime.now();

        Newsletter newsletter = new Newsletter();
        newsletter.setSubject("Preparedness update");
        newsletter.setBody("Newsletter body");
        newsletter.setStatus(NewsletterStatus.DRAFT);
        newsletter.setPreferences(Set.of(
                SubscriberPreference.PRACTICAL_SKILLS,
                SubscriberPreference.EVENTS_AND_UPDATES
        ));
        newsletter.setCreatedAt(now);
        newsletter.setUpdatedAt(now);

        Newsletter saved = newsletterRepository.saveAndFlush(newsletter);

        Newsletter reloaded = newsletterRepository
                .findById(saved.getId())
                .orElseThrow();

        assertThat(reloaded.getStatus())
                .isEqualTo(NewsletterStatus.DRAFT);

        assertThat(reloaded.getPreferences())
                .containsExactlyInAnyOrder(
                        SubscriberPreference.PRACTICAL_SKILLS,
                        SubscriberPreference.EVENTS_AND_UPDATES
                );

        assertThat(reloaded.getSentAt()).isNull();
    }
}
