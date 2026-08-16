package com.seruhioCode30.survival72.service.subscription.email;

import com.seruhioCode30.survival72.config.properties.ApplicationMailProperties;
import com.seruhioCode30.survival72.config.properties.FrontendProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionEmailService {

    private static final String WELCOME_SUBJECT = "Bienvenido a Survival72";
    private static final String UNSUBSCRIBE_SUBJECT =
            "Confirmación de cancelación de Survival72";

    private final JavaMailSender mailSender;
    private final FrontendProperties frontendProperties;
    private final ApplicationMailProperties mailProperties;

    public SubscriptionEmailService(
            JavaMailSender mailSender,
            FrontendProperties frontendProperties,
            ApplicationMailProperties mailProperties
    ) {
        this.mailSender = mailSender;
        this.frontendProperties = frontendProperties;
        this.mailProperties = mailProperties;
    }

    public void sendWelcomeEmail(
            String to,
            String rawManagementToken
    ) {
        String manageLink = buildFrontendLink(
                "manage",
                rawManagementToken
        );
        String unsubscribeLink = buildFrontendLink(
                "unsubscribe",
                rawManagementToken
        );

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.getFrom());
        message.setTo(to);
        message.setSubject(WELCOME_SUBJECT);
        message.setText(
                """
                Gracias por suscribirte a Survival72.

                Ya formas parte de nuestra comunidad de preparación ante emergencias.

                Gestionar suscripción:
                %s

                Cancelar suscripción:
                %s
                """.formatted(manageLink, unsubscribeLink)
        );

        mailSender.send(message);
    }

    public void sendUnsubscribeConfirmation(String to) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.getFrom());
        message.setTo(to);
        message.setSubject(UNSUBSCRIBE_SUBJECT);
        message.setText(
                """
                Tu suscripción a Survival72 fue cancelada correctamente.

                Ya no recibirás comunicaciones asociadas a esta suscripción.
                """
        );

        mailSender.send(message);
    }

    private String buildFrontendLink(
            String path,
            String rawManagementToken
    ) {
        if (rawManagementToken == null
                || rawManagementToken.isBlank()) {
            throw new IllegalArgumentException(
                    "management token is required for subscription links"
            );
        }

        String baseUrl = frontendProperties.getBaseUrl();

        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException(
                    "frontend base URL is not configured"
            );
        }

        String normalizedBaseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        return normalizedBaseUrl
                + "/"
                + path
                + "#token="
                + rawManagementToken;
    }
}
