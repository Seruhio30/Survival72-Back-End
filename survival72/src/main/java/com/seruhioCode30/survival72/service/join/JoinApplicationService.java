package com.seruhioCode30.survival72.service.join;

import com.seruhioCode30.survival72.service.subscription.email.SubscriptionEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

@Service
public class JoinApplicationService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JoinApplicationService.class);

    private final JoinService joinService;
    private final SubscriptionEmailService emailService;

    public JoinApplicationService(
            JoinService joinService,
            SubscriptionEmailService emailService
    ) {
        this.joinService = joinService;
        this.emailService = emailService;
    }

    public JoinResult join(JoinCommand command) {
        JoinResult result = joinService.join(command);

        if (result.outcome() == JoinOutcome.ACTIVE_DUPLICATE) {
            return result;
        }

        String rawManagementToken = result.managementToken()
                .orElseThrow(() -> new IllegalStateException(
                        "management token is required for welcome email"
                ));

        try {
            emailService.sendWelcomeEmail(
                    command.email(),
                    rawManagementToken
            );
        } catch (MailException exception) {
            LOGGER.warn(
                    "Subscription lifecycle completed but welcome email delivery failed"
            );
        }

        return result;
    }
}
