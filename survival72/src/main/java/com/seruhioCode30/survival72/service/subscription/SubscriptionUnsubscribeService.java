package com.seruhioCode30.survival72.service.subscription;

import com.seruhioCode30.survival72.model.Subscriber;
import com.seruhioCode30.survival72.model.SubscriberStatus;
import com.seruhioCode30.survival72.repository.SubscriberRepository;
import com.seruhioCode30.survival72.service.join.ManagementTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SubscriptionUnsubscribeService {

    private final SubscriberRepository subscriberRepository;
    private final ManagementTokenService managementTokenService;

    public SubscriptionUnsubscribeService(
            SubscriberRepository subscriberRepository,
            ManagementTokenService managementTokenService
    ) {
        this.subscriberRepository = subscriberRepository;
        this.managementTokenService = managementTokenService;
    }

    @Transactional
    public void unsubscribe(String rawToken) {
        String tokenHash;

        try {
            tokenHash = managementTokenService.hashToken(rawToken);
        } catch (IllegalArgumentException exception) {
            throw new SubscriptionAccessException();
        }

        Subscriber subscriber = subscriberRepository
                .findByManagementTokenHash(tokenHash)
                .orElseThrow(SubscriptionAccessException::new);

        if (subscriber.getStatus() != SubscriberStatus.ACTIVE) {
            throw new SubscriptionAccessException();
        }

        if (subscriber.getManagementTokenHash() == null) {
            throw new SubscriptionAccessException();
        }

        LocalDateTime now = LocalDateTime.now();

        subscriber.setStatus(SubscriberStatus.UNSUBSCRIBED);
        subscriber.setUnsubscribedAt(now);
        subscriber.setUpdatedAt(now);
        subscriber.setManagementTokenHash(null);

        subscriberRepository.save(subscriber);
    }
}
