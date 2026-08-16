package com.seruhioCode30.survival72.service.admin.subscriber;

import com.seruhioCode30.survival72.controller.admin.subscriber.dto.AdminSubscriberPageResponse;
import com.seruhioCode30.survival72.controller.admin.subscriber.dto.AdminSubscriberResponse;
import com.seruhioCode30.survival72.model.Subscriber;
import com.seruhioCode30.survival72.model.SubscriberPreference;
import com.seruhioCode30.survival72.model.SubscriberStatus;
import com.seruhioCode30.survival72.repository.SubscriberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Locale;

@Service
public class AdminSubscriberService {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private final SubscriberRepository subscriberRepository;

    public AdminSubscriberService(SubscriberRepository subscriberRepository) {
        this.subscriberRepository = subscriberRepository;
    }

    @Transactional(readOnly = true)
    public AdminSubscriberPageResponse findSubscribers(
            int page,
            int size,
            String statusValue,
            String preferenceValue
    ) {
        validatePagination(page, size);

        SubscriberStatus status = parseStatus(statusValue);
        SubscriberPreference preference = parsePreference(preferenceValue);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("subscribedAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<Subscriber> subscribers = findPage(
                status,
                preference,
                pageable
        );

        return new AdminSubscriberPageResponse(
                subscribers.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList(),
                subscribers.getNumber(),
                subscribers.getSize(),
                subscribers.getTotalElements(),
                subscribers.getTotalPages(),
                subscribers.hasNext()
        );
    }

    private Page<Subscriber> findPage(
            SubscriberStatus status,
            SubscriberPreference preference,
            Pageable pageable
    ) {
        if (status != null && preference != null) {
            return subscriberRepository.findByStatusAndPreference(
                    status,
                    preference,
                    pageable
            );
        }

        if (status != null) {
            return subscriberRepository.findByStatus(status, pageable);
        }

        if (preference != null) {
            return subscriberRepository.findByPreference(
                    preference,
                    pageable
            );
        }

        return subscriberRepository.findAll(pageable);
    }

    private AdminSubscriberResponse toResponse(Subscriber subscriber) {
        return new AdminSubscriberResponse(
                subscriber.getId(),
                subscriber.getEmail(),
                subscriber.getFirstName(),
                subscriber.getCountryCode(),
                subscriber.getStatus(),
                new LinkedHashSet<>(subscriber.getPreferences()),
                subscriber.getSubscribedAt(),
                subscriber.getUpdatedAt(),
                subscriber.getUnsubscribedAt()
        );
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Invalid page.");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Invalid size.");
        }
    }

    private SubscriberStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return SubscriberStatus.valueOf(
                    value.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid status.");
        }
    }

    private SubscriberPreference parsePreference(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return SubscriberPreference.valueOf(
                    value.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid preference.");
        }
    }
}
