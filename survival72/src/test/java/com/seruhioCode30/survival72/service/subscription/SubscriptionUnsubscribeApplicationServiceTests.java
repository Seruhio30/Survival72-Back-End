package com.seruhioCode30.survival72.service.subscription;

import com.seruhioCode30.survival72.service.subscription.email.SubscriptionEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.mail.MailSendException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class SubscriptionUnsubscribeApplicationServiceTests {

    private static final String RAW_TOKEN = "raw-management-token";
    private static final String EMAIL = "subscriber@example.com";

    private SubscriptionUnsubscribeService unsubscribeService;
    private SubscriptionEmailService emailService;
    private SubscriptionUnsubscribeApplicationService applicationService;

    @BeforeEach
    void setUp() {
        unsubscribeService = mock(SubscriptionUnsubscribeService.class);
        emailService = mock(SubscriptionEmailService.class);

        applicationService = new SubscriptionUnsubscribeApplicationService(
                unsubscribeService,
                emailService
        );
    }

    @Test
    void successfulUnsubscribeSendsConfirmationEmail() {
        when(unsubscribeService.unsubscribe(RAW_TOKEN))
                .thenReturn(
                        new SubscriptionUnsubscribeResult(
                                EMAIL,
                                "Sergio"
                        )
                );

        applicationService.unsubscribe(RAW_TOKEN);

        verify(emailService)
                .sendUnsubscribeConfirmation(EMAIL);
    }

    @Test
    void lifecycleCompletesBeforeEmailIsAttempted() {
        when(unsubscribeService.unsubscribe(RAW_TOKEN))
                .thenReturn(
                        new SubscriptionUnsubscribeResult(
                                EMAIL,
                                "Sergio"
                        )
                );

        applicationService.unsubscribe(RAW_TOKEN);

        InOrder inOrder = inOrder(
                unsubscribeService,
                emailService
        );

        inOrder.verify(unsubscribeService)
                .unsubscribe(RAW_TOKEN);
        inOrder.verify(emailService)
                .sendUnsubscribeConfirmation(EMAIL);
    }

    @Test
    void mailFailureDoesNotFailCompletedUnsubscribe() {
        when(unsubscribeService.unsubscribe(RAW_TOKEN))
                .thenReturn(
                        new SubscriptionUnsubscribeResult(
                                EMAIL,
                                "Sergio"
                        )
                );

        doThrow(new MailSendException("SMTP unavailable"))
                .when(emailService)
                .sendUnsubscribeConfirmation(EMAIL);

        assertThatCode(() ->
                applicationService.unsubscribe(RAW_TOKEN))
                .doesNotThrowAnyException();

        verify(unsubscribeService)
                .unsubscribe(RAW_TOKEN);
    }
}
