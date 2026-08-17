package com.seruhioCode30.survival72.service.admin.newsletter;

import com.seruhioCode30.survival72.controller.admin.newsletter.dto.AdminNewsletterAudienceMemberResponse;
import com.seruhioCode30.survival72.controller.admin.newsletter.dto.AdminNewsletterAudiencePreviewResponse;
import com.seruhioCode30.survival72.controller.admin.newsletter.dto.AdminNewsletterCreateRequest;
import com.seruhioCode30.survival72.controller.admin.newsletter.dto.AdminNewsletterPageResponse;
import com.seruhioCode30.survival72.controller.admin.newsletter.dto.AdminNewsletterResponse;
import com.seruhioCode30.survival72.controller.admin.newsletter.dto.AdminNewsletterUpdateRequest;
import com.seruhioCode30.survival72.model.Newsletter;
import com.seruhioCode30.survival72.model.NewsletterStatus;
import com.seruhioCode30.survival72.model.Subscriber;
import com.seruhioCode30.survival72.model.SubscriberStatus;
import com.seruhioCode30.survival72.model.SubscriberPreference;
import com.seruhioCode30.survival72.repository.NewsletterRepository;
import com.seruhioCode30.survival72.repository.SubscriberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Locale;

@Service
public class AdminNewsletterService {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private final NewsletterRepository newsletterRepository;
    private final SubscriberRepository subscriberRepository;

    public AdminNewsletterService(
            NewsletterRepository newsletterRepository,
            SubscriberRepository subscriberRepository
    ) {
        this.newsletterRepository = newsletterRepository;
        this.subscriberRepository = subscriberRepository;
    }

    @Transactional(readOnly = true)
    public AdminNewsletterPageResponse findNewsletters(
            int page,
            int size,
            String statusValue
    ) {
        validatePagination(page, size);

        NewsletterStatus status = parseStatus(statusValue);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<Newsletter> newsletters = status == null
                ? newsletterRepository.findAll(pageable)
                : newsletterRepository.findByStatus(status, pageable);

        return new AdminNewsletterPageResponse(
                newsletters.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList(),
                newsletters.getNumber(),
                newsletters.getSize(),
                newsletters.getTotalElements(),
                newsletters.getTotalPages(),
                newsletters.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public AdminNewsletterResponse findById(Long id) {
        return toResponse(findEntity(id));
    }

    @Transactional
    public AdminNewsletterResponse create(
            AdminNewsletterCreateRequest request
    ) {
        LocalDateTime now = LocalDateTime.now();

        Newsletter newsletter = new Newsletter();
        newsletter.setSubject(normalizeRequired(request.subject(), "Subject is required."));
        newsletter.setBody(normalizeRequired(request.body(), "Body is required."));
        newsletter.setPreferences(requirePreferences(request.preferences()));
        newsletter.setStatus(NewsletterStatus.DRAFT);
        newsletter.setCreatedAt(now);
        newsletter.setUpdatedAt(now);
        newsletter.setSentAt(null);

        return toResponse(newsletterRepository.save(newsletter));
    }

    @Transactional
    public AdminNewsletterResponse update(
            Long id,
            AdminNewsletterUpdateRequest request
    ) {
        Newsletter newsletter = findEntity(id);

        if (newsletter.getStatus() == NewsletterStatus.SENT) {
            throw new IllegalArgumentException(
                    "Sent newsletter cannot be modified."
            );
        }

        boolean changed = false;

        if (request.subject() != null) {
            newsletter.setSubject(
                    normalizeRequired(
                            request.subject(),
                            "Subject is required."
                    )
            );
            changed = true;
        }

        if (request.body() != null) {
            newsletter.setBody(
                    normalizeRequired(
                            request.body(),
                            "Body is required."
                    )
            );
            changed = true;
        }

        if (request.preferences() != null) {
            newsletter.setPreferences(
                    requirePreferences(request.preferences())
            );
            changed = true;
        }

        if (changed
                && newsletter.getStatus() == NewsletterStatus.READY_TO_SEND) {
            newsletter.setStatus(NewsletterStatus.DRAFT);
        }

        if (changed) {
            newsletter.setUpdatedAt(LocalDateTime.now());
        }

        return toResponse(newsletterRepository.save(newsletter));
    }

    @Transactional
    public AdminNewsletterResponse markReady(Long id) {
        Newsletter newsletter = findEntity(id);

        if (newsletter.getStatus() == NewsletterStatus.SENT) {
            throw new IllegalArgumentException(
                    "Sent newsletter cannot be modified."
            );
        }

        validateReady(newsletter);

        newsletter.setStatus(NewsletterStatus.READY_TO_SEND);
        newsletter.setUpdatedAt(LocalDateTime.now());

        return toResponse(newsletterRepository.save(newsletter));
    }

    @Transactional(readOnly = true)
    public AdminNewsletterAudiencePreviewResponse previewAudience(
            Long id,
            int page,
            int size
    ) {
        validatePagination(page, size);

        Newsletter newsletter = findEntity(id);
        validateReady(newsletter);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("subscribedAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<Subscriber> audience =
                subscriberRepository.findDistinctByStatusAndPreferencesIn(
                        SubscriberStatus.ACTIVE,
                        newsletter.getPreferences(),
                        pageable
                );

        return new AdminNewsletterAudiencePreviewResponse(
                audience.getTotalElements(),
                audience.getContent()
                        .stream()
                        .map(this::toAudienceMemberResponse)
                        .toList(),
                audience.getNumber(),
                audience.getSize(),
                audience.getTotalPages(),
                audience.hasNext()
        );
    }

    private Newsletter findEntity(Long id) {
        return newsletterRepository.findById(id)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Newsletter not found."
                        )
                );
    }

    private AdminNewsletterResponse toResponse(Newsletter newsletter) {
        return new AdminNewsletterResponse(
                newsletter.getId(),
                newsletter.getSubject(),
                newsletter.getBody(),
                newsletter.getStatus(),
                new LinkedHashSet<>(newsletter.getPreferences()),
                newsletter.getCreatedAt(),
                newsletter.getUpdatedAt(),
                newsletter.getSentAt()
        );
    }

    private AdminNewsletterAudienceMemberResponse toAudienceMemberResponse(
            Subscriber subscriber
    ) {
        return new AdminNewsletterAudienceMemberResponse(
                subscriber.getId(),
                subscriber.getEmail(),
                subscriber.getFirstName(),
                new LinkedHashSet<>(subscriber.getPreferences())
        );
    }

    private void validateReady(Newsletter newsletter) {
        normalizeRequired(
                newsletter.getSubject(),
                "Subject is required."
        );
        normalizeRequired(
                newsletter.getBody(),
                "Body is required."
        );
        requirePreferences(newsletter.getPreferences());
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

    private LinkedHashSet<SubscriberPreference> requirePreferences(
            java.util.Set<SubscriberPreference> preferences
    ) {
        if (preferences == null || preferences.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one preference is required."
            );
        }

        return new LinkedHashSet<>(preferences);
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Invalid page.");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Invalid size.");
        }
    }

    private NewsletterStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return NewsletterStatus.valueOf(
                    value.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid newsletter status."
            );
        }
    }
}
