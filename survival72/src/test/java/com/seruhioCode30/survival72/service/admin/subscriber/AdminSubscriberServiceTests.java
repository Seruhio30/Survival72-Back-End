package com.seruhioCode30.survival72.service.admin.subscriber;

import com.seruhioCode30.survival72.controller.admin.subscriber.dto.AdminSubscriberPageResponse;
import com.seruhioCode30.survival72.model.Subscriber;
import com.seruhioCode30.survival72.model.SubscriberPreference;
import com.seruhioCode30.survival72.model.SubscriberStatus;
import com.seruhioCode30.survival72.repository.SubscriberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSubscriberServiceTests {

    @Mock
    private SubscriberRepository subscriberRepository;

    @Test
    void returnsPagedSubscribersUsingStableDescendingOrder() {
        AdminSubscriberService service =
                new AdminSubscriberService(subscriberRepository);

        Subscriber subscriber = createSubscriber(
                1L,
                "admin-view@example.com",
                SubscriberStatus.ACTIVE,
                Set.of(SubscriberPreference.EMERGENCY_KIT),
                LocalDateTime.of(2026, 8, 16, 12, 0)
        );

        when(subscriberRepository.findAll(
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).thenAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(0);
            return new PageImpl<>(
                    List.of(subscriber),
                    pageable,
                    1
            );
        });

        AdminSubscriberPageResponse response =
                service.findSubscribers(0, 20, null, null);

        assertThat(response.content()).hasSize(1);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();

        assertThat(response.content().get(0).email())
                .isEqualTo("admin-view@example.com");
        assertThat(response.content().get(0).preferences())
                .containsExactly(SubscriberPreference.EMERGENCY_KIT);

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(subscriberRepository).findAll(pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().getOrderFor("subscribedAt"))
                .isNotNull()
                .extracting(order -> order.getDirection().name())
                .isEqualTo("DESC");
        assertThat(pageable.getSort().getOrderFor("id"))
                .isNotNull()
                .extracting(order -> order.getDirection().name())
                .isEqualTo("DESC");
    }

    @Test
    void delegatesStatusFilterToRepository() {
        AdminSubscriberService service =
                new AdminSubscriberService(subscriberRepository);

        when(subscriberRepository.findByStatus(
                eq(SubscriberStatus.ACTIVE),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).thenAnswer(invocation -> new PageImpl<>(
                List.of(),
                invocation.getArgument(1),
                0
        ));

        service.findSubscribers(0, 20, "ACTIVE", null);

        verify(subscriberRepository).findByStatus(
                eq(SubscriberStatus.ACTIVE),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        );
    }

    @Test
    void delegatesPreferenceFilterToRepository() {
        AdminSubscriberService service =
                new AdminSubscriberService(subscriberRepository);

        when(subscriberRepository.findByPreference(
                eq(SubscriberPreference.EMERGENCY_KIT),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).thenAnswer(invocation -> new PageImpl<>(
                List.of(),
                invocation.getArgument(1),
                0
        ));

        service.findSubscribers(0, 20, null, "EMERGENCY_KIT");

        verify(subscriberRepository).findByPreference(
                eq(SubscriberPreference.EMERGENCY_KIT),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        );
    }

    @Test
    void delegatesCombinedFiltersToRepository() {
        AdminSubscriberService service =
                new AdminSubscriberService(subscriberRepository);

        when(subscriberRepository.findByStatusAndPreference(
                eq(SubscriberStatus.ACTIVE),
                eq(SubscriberPreference.PRACTICAL_SKILLS),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).thenAnswer(invocation -> new PageImpl<>(
                List.of(),
                invocation.getArgument(2),
                0
        ));

        service.findSubscribers(
                0,
                20,
                "ACTIVE",
                "PRACTICAL_SKILLS"
        );

        verify(subscriberRepository).findByStatusAndPreference(
                eq(SubscriberStatus.ACTIVE),
                eq(SubscriberPreference.PRACTICAL_SKILLS),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        );
    }

    @Test
    void rejectsInvalidStatus() {
        AdminSubscriberService service =
                new AdminSubscriberService(subscriberRepository);

        assertThatThrownBy(() ->
                service.findSubscribers(0, 20, "INVALID", null)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidPreference() {
        AdminSubscriberService service =
                new AdminSubscriberService(subscriberRepository);

        assertThatThrownBy(() ->
                service.findSubscribers(0, 20, null, "INVALID")
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidPageAndSize() {
        AdminSubscriberService service =
                new AdminSubscriberService(subscriberRepository);

        assertThatThrownBy(() ->
                service.findSubscribers(-1, 20, null, null)
        ).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() ->
                service.findSubscribers(0, 0, null, null)
        ).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() ->
                service.findSubscribers(
                        0,
                        AdminSubscriberService.MAX_PAGE_SIZE + 1,
                        null,
                        null
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }

    private Subscriber createSubscriber(
            Long ignoredId,
            String email,
            SubscriberStatus status,
            Set<SubscriberPreference> preferences,
            LocalDateTime subscribedAt
    ) {
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(email);
        subscriber.setFirstName("Admin View");
        subscriber.setCountryCode("CR");
        subscriber.setStatus(status);
        subscriber.setPreferences(preferences);
        subscriber.setSubscribedAt(subscribedAt);
        subscriber.setUpdatedAt(subscribedAt);
        subscriber.setManagementTokenHash(
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        );
        return subscriber;
    }
}
