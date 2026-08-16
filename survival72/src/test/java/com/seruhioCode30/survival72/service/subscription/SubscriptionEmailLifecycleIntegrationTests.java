package com.seruhioCode30.survival72.service.subscription;

import com.seruhioCode30.survival72.model.Subscriber;
import com.seruhioCode30.survival72.model.SubscriberPreference;
import com.seruhioCode30.survival72.model.SubscriberStatus;
import com.seruhioCode30.survival72.repository.SubscriberRepository;
import com.seruhioCode30.survival72.service.join.JoinApplicationService;
import com.seruhioCode30.survival72.service.join.JoinCommand;
import com.seruhioCode30.survival72.service.join.JoinService;
import com.seruhioCode30.survival72.service.join.ManagementTokenService;
import com.seruhioCode30.survival72.service.subscription.email.SubscriptionEmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

@DataJpaTest
@Import({
        JoinService.class,
        JoinApplicationService.class,
        ManagementTokenService.class,
        SubscriptionUnsubscribeService.class,
        SubscriptionUnsubscribeApplicationService.class
})
@TestPropertySource(properties = {
        "spring.test.database.replace=NONE",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SubscriptionEmailLifecycleIntegrationTests {

    @Autowired
    private JoinApplicationService joinApplicationService;

    @Autowired
    private SubscriptionUnsubscribeApplicationService
            unsubscribeApplicationService;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private ManagementTokenService managementTokenService;

    @MockBean
    private SubscriptionEmailService emailService;

    @Test
    void smtpFailureDoesNotRollbackCompletedNewJoin() {
        String email = "smtp-failure-join@example.com";

        doThrow(new MailSendException("SMTP unavailable"))
                .when(emailService)
                .sendWelcomeEmail(anyString(), anyString());

        assertThatCode(() ->
                joinApplicationService.join(
                        new JoinCommand(
                                email,
                                "Sergio",
                                "CR",
                                Set.of(
                                        SubscriberPreference
                                                .GENERAL_PREPAREDNESS
                                )
                        )
                )
        ).doesNotThrowAnyException();

        Subscriber persisted = subscriberRepository
                .findByEmail(email)
                .orElseThrow();

        assertThat(persisted.getStatus())
                .isEqualTo(SubscriberStatus.ACTIVE);
        assertThat(persisted.getManagementTokenHash())
                .matches("^[0-9a-f]{64}$");

        subscriberRepository.deleteById(persisted.getId());
    }

    @Test
    void smtpFailureDoesNotRollbackCompletedUnsubscribe() {
        String email = "smtp-failure-unsubscribe@example.com";
        String rawToken = managementTokenService.generateToken();
        LocalDateTime now = LocalDateTime.now().minusDays(1);

        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(email);
        subscriber.setFirstName("Sergio");
        subscriber.setCountryCode("CR");
        subscriber.setStatus(SubscriberStatus.ACTIVE);
        subscriber.setSubscribedAt(now);
        subscriber.setUpdatedAt(now);
        subscriber.setUnsubscribedAt(null);
        subscriber.setManagementTokenHash(
                managementTokenService.hashToken(rawToken)
        );
        subscriber.setPreferences(Set.of(
                SubscriberPreference.GENERAL_PREPAREDNESS
        ));

        Subscriber persisted =
                subscriberRepository.saveAndFlush(subscriber);

        doThrow(new MailSendException("SMTP unavailable"))
                .when(emailService)
                .sendUnsubscribeConfirmation(email);

        assertThatCode(() ->
                unsubscribeApplicationService.unsubscribe(rawToken)
        ).doesNotThrowAnyException();

        Subscriber reloaded = subscriberRepository
                .findById(persisted.getId())
                .orElseThrow();

        assertThat(reloaded.getStatus())
                .isEqualTo(SubscriberStatus.UNSUBSCRIBED);
        assertThat(reloaded.getUnsubscribedAt()).isNotNull();
        assertThat(reloaded.getManagementTokenHash()).isNull();

        subscriberRepository.deleteById(reloaded.getId());
    }
}
