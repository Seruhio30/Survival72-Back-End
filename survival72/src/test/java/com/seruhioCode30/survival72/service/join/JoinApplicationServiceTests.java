package com.seruhioCode30.survival72.service.join;

import com.seruhioCode30.survival72.model.SubscriberPreference;
import com.seruhioCode30.survival72.service.subscription.email.SubscriptionEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.mail.MailSendException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JoinApplicationServiceTests {

    private static final String EMAIL = "subscriber@example.com";
    private static final String NEW_TOKEN = "new-management-token";
    private static final String REJOIN_TOKEN = "rejoin-management-token";

    private JoinService joinService;
    private SubscriptionEmailService emailService;
    private JoinApplicationService applicationService;
    private JoinCommand command;

    @BeforeEach
    void setUp() {
        joinService = mock(JoinService.class);
        emailService = mock(SubscriptionEmailService.class);

        applicationService = new JoinApplicationService(
                joinService,
                emailService
        );

        command = new JoinCommand(
                EMAIL,
                "Sergio",
                "CR",
                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
        );
    }

    @Test
    void newSubscriptionSendsWelcomeEmailWithGeneratedToken() {
        JoinResult expected = new JoinResult(
                JoinOutcome.NEW_SUBSCRIPTION,
                NEW_TOKEN
        );

        when(joinService.join(command)).thenReturn(expected);

        JoinResult actual = applicationService.join(command);

        assertThat(actual).isEqualTo(expected);

        verify(emailService)
                .sendWelcomeEmail(EMAIL, NEW_TOKEN);
    }

    @Test
    void rejoinedSubscriptionSendsWelcomeEmailWithNewToken() {
        JoinResult expected = new JoinResult(
                JoinOutcome.REJOINED,
                REJOIN_TOKEN
        );

        when(joinService.join(command)).thenReturn(expected);

        applicationService.join(command);

        verify(emailService)
                .sendWelcomeEmail(EMAIL, REJOIN_TOKEN);
    }

    @Test
    void activeDuplicateDoesNotSendWelcomeEmail() {
        JoinResult expected = new JoinResult(
                JoinOutcome.ACTIVE_DUPLICATE,
                null
        );

        when(joinService.join(command)).thenReturn(expected);

        JoinResult actual = applicationService.join(command);

        assertThat(actual).isEqualTo(expected);

        verify(emailService, never())
                .sendWelcomeEmail(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString()
                );
    }

    @Test
    void lifecycleCompletesBeforeWelcomeEmailIsAttempted() {
        when(joinService.join(command)).thenReturn(
                new JoinResult(
                        JoinOutcome.NEW_SUBSCRIPTION,
                        NEW_TOKEN
                )
        );

        applicationService.join(command);

        InOrder inOrder = inOrder(
                joinService,
                emailService
        );

        inOrder.verify(joinService).join(command);
        inOrder.verify(emailService)
                .sendWelcomeEmail(EMAIL, NEW_TOKEN);
    }

    @Test
    void mailFailureDoesNotFailCompletedJoin() {
        JoinResult expected = new JoinResult(
                JoinOutcome.NEW_SUBSCRIPTION,
                NEW_TOKEN
        );

        when(joinService.join(command)).thenReturn(expected);

        doThrow(new MailSendException("SMTP unavailable"))
                .when(emailService)
                .sendWelcomeEmail(EMAIL, NEW_TOKEN);

        assertThatCode(() -> applicationService.join(command))
                .doesNotThrowAnyException();

        verify(joinService).join(command);
    }
}
