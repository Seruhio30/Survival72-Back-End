package com.seruhioCode30.survival72.service.admin.content;

import com.seruhioCode30.survival72.controller.admin.content.dto.AdminContentCreateRequest;
import com.seruhioCode30.survival72.controller.admin.content.dto.AdminContentResponse;
import com.seruhioCode30.survival72.controller.admin.content.dto.AdminContentUpdateRequest;
import com.seruhioCode30.survival72.model.ContentItem;
import com.seruhioCode30.survival72.model.ContentStatus;
import com.seruhioCode30.survival72.model.ContentType;
import com.seruhioCode30.survival72.model.SubscriberPreference;
import com.seruhioCode30.survival72.repository.ContentItemRepository;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminContentServiceTests {

    private ContentItemRepository repository;
    private AdminContentService service;

    @BeforeEach
    void setUp() {
        repository = mock(ContentItemRepository.class);
        service = new AdminContentService(
                repository,
                new YouTubeVideoIdNormalizer()
        );

        when(repository.save(any(ContentItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsDraftArticleWithoutYoutubeVideo() {
        AdminContentResponse response = service.create(
                new AdminContentCreateRequest(
                        ContentType.ARTICLE,
                        " Emergency Plan ",
                        " Family plan ",
                        null,
                        null,
                        Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
                )
        );

        assertThat(response.type()).isEqualTo(ContentType.ARTICLE);
        assertThat(response.status()).isEqualTo(ContentStatus.DRAFT);
        assertThat(response.title()).isEqualTo("Emergency Plan");
        assertThat(response.description()).isEqualTo("Family plan");
        assertThat(response.youtubeVideoId()).isNull();
        assertThat(response.publishedAt()).isNull();
        assertThat(response.preferences())
                .containsExactly(SubscriberPreference.GENERAL_PREPAREDNESS);
    }

    @Test
    void createsVideoFromCanonicalId() {
        AdminContentResponse response = service.create(
                new AdminContentCreateRequest(
                        ContentType.VIDEO,
                        "Water",
                        null,
                        "dQw4w9WgXcQ",
                        ContentStatus.DRAFT,
                        Set.of()
                )
        );

        assertThat(response.youtubeVideoId())
                .isEqualTo("dQw4w9WgXcQ");
    }

    @Test
    void createsVideoFromYoutubeUrlAndStoresOnlyCanonicalId() {
        AdminContentResponse response = service.create(
                new AdminContentCreateRequest(
                        ContentType.VIDEO,
                        "Water",
                        null,
                        "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                        ContentStatus.DRAFT,
                        Set.of()
                )
        );

        assertThat(response.youtubeVideoId())
                .isEqualTo("dQw4w9WgXcQ");
    }

    @Test
    void createsVideoFromShortYoutubeUrl() {
        AdminContentResponse response = service.create(
                new AdminContentCreateRequest(
                        ContentType.VIDEO,
                        "Water",
                        null,
                        "https://youtu.be/dQw4w9WgXcQ",
                        ContentStatus.DRAFT,
                        Set.of()
                )
        );

        assertThat(response.youtubeVideoId())
                .isEqualTo("dQw4w9WgXcQ");
    }

    @Test
    void invalidVideoIsRejected() {
        assertThatThrownBy(() -> service.create(
                new AdminContentCreateRequest(
                        ContentType.VIDEO,
                        "Water",
                        null,
                        "not-a-valid-video",
                        ContentStatus.DRAFT,
                        Set.of()
                )
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void articleWithYoutubeValueIsRejected() {
        assertThatThrownBy(() -> service.create(
                new AdminContentCreateRequest(
                        ContentType.ARTICLE,
                        "Article",
                        null,
                        "dQw4w9WgXcQ",
                        ContentStatus.DRAFT,
                        Set.of()
                )
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void publishingExistingDraftSetsPublishedAt() {
        ContentItem item = content(
                ContentType.ARTICLE,
                ContentStatus.DRAFT,
                null
        );

        when(repository.findById(5L)).thenReturn(Optional.of(item));

        AdminContentResponse response = service.update(
                5L,
                new AdminContentUpdateRequest(
                        null,
                        null,
                        null,
                        null,
                        ContentStatus.PUBLISHED,
                        null
                )
        );

        assertThat(response.status()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(response.publishedAt()).isNotNull();
    }

    @Test
    void archivingPreservesPublishedAtAndContent() {
        LocalDateTime publishedAt =
                LocalDateTime.of(2026, 8, 16, 10, 0);

        ContentItem item = content(
                ContentType.ARTICLE,
                ContentStatus.PUBLISHED,
                null
        );
        item.setPublishedAt(publishedAt);
        item.setDescription("Keep this");

        when(repository.findById(6L)).thenReturn(Optional.of(item));

        AdminContentResponse response = service.update(
                6L,
                new AdminContentUpdateRequest(
                        null,
                        null,
                        null,
                        null,
                        ContentStatus.ARCHIVED,
                        null
                )
        );

        assertThat(response.status()).isEqualTo(ContentStatus.ARCHIVED);
        assertThat(response.publishedAt()).isEqualTo(publishedAt);
        assertThat(response.description()).isEqualTo("Keep this");
    }

    @Test
    void updatePersistsPreferences() {
        ContentItem item = content(
                ContentType.ARTICLE,
                ContentStatus.DRAFT,
                null
        );

        when(repository.findById(7L)).thenReturn(Optional.of(item));

        AdminContentResponse response = service.update(
                7L,
                new AdminContentUpdateRequest(
                        null,
                        "Updated",
                        null,
                        null,
                        null,
                        Set.of(
                                SubscriberPreference.EMERGENCY_KIT,
                                SubscriberPreference.EDUCATIONAL_CONTENT
                        )
                )
        );

        assertThat(response.title()).isEqualTo("Updated");
        assertThat(response.preferences()).containsExactlyInAnyOrder(
                SubscriberPreference.EMERGENCY_KIT,
                SubscriberPreference.EDUCATIONAL_CONTENT
        );
    }

    @Test
    void listUsesRequestedPaginationAndCreatedAtDescendingOrder() {
        ContentItem item = content(
                ContentType.ARTICLE,
                ContentStatus.DRAFT,
                null
        );

        when(repository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item)));

        service.findContent(0, 20, null, null);

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(repository).findAll(captor.capture());

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
    void listFiltersByTypeAndStatus() {
        when(repository.findByTypeAndStatus(
                any(ContentType.class),
                any(ContentStatus.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        service.findContent(
                0,
                20,
                "video",
                "published"
        );

        verify(repository).findByTypeAndStatus(
                org.mockito.ArgumentMatchers.eq(ContentType.VIDEO),
                org.mockito.ArgumentMatchers.eq(ContentStatus.PUBLISHED),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        );
    }

    @Test
    void pageSizeAboveMaximumIsRejected() {
        assertThatThrownBy(
                () -> service.findContent(0, 101, null, null)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    private ContentItem content(
            ContentType type,
            ContentStatus status,
            String youtubeVideoId
    ) {
        ContentItem item = new ContentItem();
        item.setType(type);
        item.setStatus(status);
        item.setTitle("Content");
        item.setYoutubeVideoId(youtubeVideoId);
        item.setPreferences(Set.of());
        item.setCreatedAt(LocalDateTime.of(2026, 8, 16, 9, 0));
        item.setUpdatedAt(LocalDateTime.of(2026, 8, 16, 9, 0));
        return item;
    }
}
