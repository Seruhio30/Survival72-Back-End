package com.seruhioCode30.survival72.service.admin.newsletter;

import com.seruhioCode30.survival72.controller.admin.newsletter.dto.AdminNewsletterCreateRequest;
import com.seruhioCode30.survival72.controller.admin.newsletter.dto.AdminNewsletterResponse;
import com.seruhioCode30.survival72.controller.admin.newsletter.dto.AdminNewsletterUpdateRequest;
import com.seruhioCode30.survival72.model.Newsletter;
import com.seruhioCode30.survival72.model.NewsletterStatus;
import com.seruhioCode30.survival72.model.SubscriberPreference;
import com.seruhioCode30.survival72.model.SubscriberStatus;
import com.seruhioCode30.survival72.repository.NewsletterRepository;
import com.seruhioCode30.survival72.repository.SubscriberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminNewsletterServiceTests {

    private NewsletterRepository newsletterRepository;
    private SubscriberRepository subscriberRepository;
    private AdminNewsletterService service;

    @BeforeEach
    void setUp() {
        newsletterRepository = mock(NewsletterRepository.class);
        subscriberRepository = mock(SubscriberRepository.class);

        service = new AdminNewsletterService(
                newsletterRepository,
                subscriberRepository
        );

        when(newsletterRepository.save(any(Newsletter.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsDraftNewsletter() {
        AdminNewsletterResponse response = service.create(
                new AdminNewsletterCreateRequest(
                        " Emergency kit ",
                        " Prepare supplies ",
                        Set.of(SubscriberPreference.EMERGENCY_KIT)
                )
        );

        assertThat(response.subject()).isEqualTo("Emergency kit");
        assertThat(response.body()).isEqualTo("Prepare supplies");
        assertThat(response.status()).isEqualTo(NewsletterStatus.DRAFT);
        assertThat(response.sentAt()).isNull();
        assertThat(response.preferences())
                .containsExactly(SubscriberPreference.EMERGENCY_KIT);
    }

    @Test
    void createRequiresPreferences() {
        assertThatThrownBy(() -> service.create(
                new AdminNewsletterCreateRequest(
                        "Subject",
                        "Body",
                        Set.of()
                )
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void editsDraftNewsletter() {
        Newsletter newsletter = newsletter(
                NewsletterStatus.DRAFT,
                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
        );

        when(newsletterRepository.findById(1L))
                .thenReturn(Optional.of(newsletter));

        AdminNewsletterResponse response = service.update(
                1L,
                new AdminNewsletterUpdateRequest(
                        "Updated",
                        "Updated body",
                        Set.of(
                                SubscriberPreference.PRACTICAL_SKILLS,
                                SubscriberPreference.EVENTS_AND_UPDATES
                        )
                )
        );

        assertThat(response.subject()).isEqualTo("Updated");
        assertThat(response.body()).isEqualTo("Updated body");
        assertThat(response.preferences()).containsExactlyInAnyOrder(
                SubscriberPreference.PRACTICAL_SKILLS,
                SubscriberPreference.EVENTS_AND_UPDATES
        );
        assertThat(response.status()).isEqualTo(NewsletterStatus.DRAFT);
    }

    @Test
    void editingReadyNewsletterReturnsItToDraft() {
        Newsletter newsletter = newsletter(
                NewsletterStatus.READY_TO_SEND,
                Set.of(SubscriberPreference.EMERGENCY_KIT)
        );

        when(newsletterRepository.findById(2L))
                .thenReturn(Optional.of(newsletter));

        AdminNewsletterResponse response = service.update(
                2L,
                new AdminNewsletterUpdateRequest(
                        "Changed",
                        null,
                        null
                )
        );

        assertThat(response.status()).isEqualTo(NewsletterStatus.DRAFT);
    }

    @Test
    void sentNewsletterIsImmutable() {
        Newsletter newsletter = newsletter(
                NewsletterStatus.SENT,
                Set.of(SubscriberPreference.EMERGENCY_KIT)
        );

        when(newsletterRepository.findById(3L))
                .thenReturn(Optional.of(newsletter));

        assertThatThrownBy(() -> service.update(
                3L,
                new AdminNewsletterUpdateRequest(
                        "Changed",
                        null,
                        null
                )
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void marksValidDraftReadyToSend() {
        Newsletter newsletter = newsletter(
                NewsletterStatus.DRAFT,
                Set.of(SubscriberPreference.PRACTICAL_SKILLS)
        );

        when(newsletterRepository.findById(4L))
                .thenReturn(Optional.of(newsletter));

        AdminNewsletterResponse response = service.markReady(4L);

        assertThat(response.status())
                .isEqualTo(NewsletterStatus.READY_TO_SEND);
        assertThat(response.sentAt()).isNull();
    }

    @Test
    void listUsesPaginationAndCreatedAtDescendingOrder() {
        Newsletter newsletter = newsletter(
                NewsletterStatus.DRAFT,
                Set.of(SubscriberPreference.EMERGENCY_KIT)
        );

        when(newsletterRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(newsletter)));

        service.findNewsletters(0, 20, null);

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(newsletterRepository).findAll(captor.capture());

        Pageable pageable = captor.getValue();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().getOrderFor("createdAt"))
                .isNotNull();
        assertThat(pageable.getSort().getOrderFor("createdAt").isDescending())
                .isTrue();
        assertThat(pageable.getSort().getOrderFor("id"))
                .isNotNull();
        assertThat(pageable.getSort().getOrderFor("id").isDescending())
                .isTrue();
    }

    @Test
    void listCanFilterByStatus() {
        when(newsletterRepository.findByStatus(
                eq(NewsletterStatus.READY_TO_SEND),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        service.findNewsletters(
                0,
                20,
                "ready_to_send"
        );

        verify(newsletterRepository).findByStatus(
                eq(NewsletterStatus.READY_TO_SEND),
                any(Pageable.class)
        );
    }

    @Test
    void audiencePreviewUsesActiveAnyPreferenceQuery() {
        Newsletter newsletter = newsletter(
                NewsletterStatus.DRAFT,
                Set.of(
                        SubscriberPreference.EMERGENCY_KIT,
                        SubscriberPreference.PRACTICAL_SKILLS
                )
        );

        when(newsletterRepository.findById(5L))
                .thenReturn(Optional.of(newsletter));

        when(subscriberRepository.findDistinctByStatusAndPreferencesIn(
                eq(SubscriberStatus.ACTIVE),
                eq(newsletter.getPreferences()),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        service.previewAudience(5L, 0, 20);

        verify(subscriberRepository)
                .findDistinctByStatusAndPreferencesIn(
                        eq(SubscriberStatus.ACTIVE),
                        eq(newsletter.getPreferences()),
                        any(Pageable.class)
                );
    }

    @Test
    void pageSizeAboveMaximumIsRejected() {
        assertThatThrownBy(
                () -> service.findNewsletters(0, 101, null)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    private Newsletter newsletter(
            NewsletterStatus status,
            Set<SubscriberPreference> preferences
    ) {
        Newsletter newsletter = new Newsletter();
        newsletter.setSubject("Subject");
        newsletter.setBody("Body");
        newsletter.setStatus(status);
        newsletter.setPreferences(preferences);
        newsletter.setCreatedAt(
                LocalDateTime.of(2026, 8, 17, 9, 0)
        );
        newsletter.setUpdatedAt(
                LocalDateTime.of(2026, 8, 17, 9, 0)
        );
        return newsletter;
    }
}
