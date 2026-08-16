package com.seruhioCode30.survival72.service.subscription;

import com.seruhioCode30.survival72.service.subscription.email.SubscriptionEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionUnsubscribeApplicationService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    SubscriptionUnsubscribeApplicationService.class
            );

    private final SubscriptionUnsubscribeService unsubscribeService;
    private final SubscriptionEmailService emailService;

    public SubscriptionUnsubscribeApplicationService(
            SubscriptionUnsubscribeService unsubscribeService,
            SubscriptionEmailService emailService
    ) {
        this.unsubscribeService = unsubscribeService;
        this.emailService = emailService;
    }

    public void unsubscribe(String rawManagementToken) {
        SubscriptionUnsubscribeResult result =
                unsubscribeService.unsubscribe(rawManagementToken);

        try {
            emailService.sendUnsubscribeConfirmation(
                    result.email()
            );
        } catch (MailException exception) {
            LOGGER.warn(
                    "Subscription was cancelled but confirmation email delivery failed"
            );
        }
    }
}
