package com.seruhioCode30.survival72.service.subscription.email;

import com.seruhioCode30.survival72.config.properties.ApplicationMailProperties;
import com.seruhioCode30.survival72.config.properties.FrontendProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SubscriptionEmailServiceTests {

    private static final String FRONTEND_BASE_URL =
            "https://frontend.example";
    private static final String FROM =
            "no-reply@example.com";
    private static final String TO =
            "subscriber@example.com";
    private static final String TOKEN =
            "new-management-token";

    private JavaMailSender mailSender;
    private SubscriptionEmailService emailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);

        FrontendProperties frontendProperties =
                new FrontendProperties();
        frontendProperties.setBaseUrl(FRONTEND_BASE_URL);

        ApplicationMailProperties mailProperties =
                new ApplicationMailProperties();
        mailProperties.setFrom(FROM);

        emailService = new SubscriptionEmailService(
                mailSender,
                frontendProperties,
                mailProperties
        );
    }

    @Test
    void welcomeEmailContainsManageLink() {
        SimpleMailMessage message = sendWelcomeAndCapture();

        assertThat(message.getText())
                .contains(
                        FRONTEND_BASE_URL
                                + "/manage#token="
                                + TOKEN
                );
    }

    @Test
    void welcomeEmailContainsUnsubscribeLink() {
        SimpleMailMessage message = sendWelcomeAndCapture();

        assertThat(message.getText())
                .contains(
                        FRONTEND_BASE_URL
                                + "/unsubscribe#token="
                                + TOKEN
                );
    }

    @Test
    void managementLinksUseFragmentToken() {
        SimpleMailMessage message = sendWelcomeAndCapture();

        assertThat(message.getText())
                .contains("/manage#token=" + TOKEN)
                .contains("/unsubscribe#token=" + TOKEN);
    }

    @Test
    void managementLinksDoNotUseTokenQueryParameter() {
        SimpleMailMessage message = sendWelcomeAndCapture();

        assertThat(message.getText())
                .doesNotContain("?token=")
                .doesNotContain("&token=");
    }

    @Test
    void managementLinksDoNotContainSubscriberEmail() {
        SimpleMailMessage message = sendWelcomeAndCapture();

        assertThat(message.getText())
                .doesNotContain(TO);
    }

    @Test
    void configuredFrontendBaseUrlIsUsed() {
        SimpleMailMessage message = sendWelcomeAndCapture();

        assertThat(message.getText())
                .contains(FRONTEND_BASE_URL + "/manage")
                .contains(FRONTEND_BASE_URL + "/unsubscribe");
    }

    @Test
    void trailingSlashIsNormalized() {
        FrontendProperties frontendProperties =
                new FrontendProperties();
        frontendProperties.setBaseUrl(
                "https://frontend.example/"
        );

        ApplicationMailProperties mailProperties =
                new ApplicationMailProperties();
        mailProperties.setFrom(FROM);

        emailService = new SubscriptionEmailService(
                mailSender,
                frontendProperties,
                mailProperties
        );

        SimpleMailMessage message = sendWelcomeAndCapture();

        assertThat(message.getText())
                .contains(
                        "https://frontend.example/manage#token="
                                + TOKEN
                )
                .doesNotContain(
                        "https://frontend.example//manage"
                );
    }

    @Test
    void unsubscribeConfirmationDoesNotRequireManagementToken() {
        emailService.sendUnsubscribeConfirmation(TO);

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();

        assertThat(message.getTo()).containsExactly(TO);
        assertThat(message.getText())
                .contains("cancelada correctamente")
                .doesNotContain("token=")
                .doesNotContain("/manage")
                .doesNotContain("/unsubscribe");
    }

    private SimpleMailMessage sendWelcomeAndCapture() {
        emailService.sendWelcomeEmail(TO, TOKEN);

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(captor.capture());

        return captor.getValue();
    }
}
