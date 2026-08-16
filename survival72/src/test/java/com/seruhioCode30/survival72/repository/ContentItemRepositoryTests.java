package com.seruhioCode30.survival72.repository;

import com.seruhioCode30.survival72.model.ContentItem;
import com.seruhioCode30.survival72.model.ContentStatus;
import com.seruhioCode30.survival72.model.ContentType;
import com.seruhioCode30.survival72.model.SubscriberPreference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.test.database.replace=NONE",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class ContentItemRepositoryTests {

    @Autowired
    private ContentItemRepository contentItemRepository;

    @Test
    void persistsContentItemAndPreferences() {
        ContentItem item = createContentItem(
                ContentType.ARTICLE,
                ContentStatus.DRAFT
        );
        item.setPreferences(Set.of(
                SubscriberPreference.GENERAL_PREPAREDNESS,
                SubscriberPreference.PRACTICAL_SKILLS
        ));

        ContentItem saved = contentItemRepository.saveAndFlush(item);

        ContentItem reloaded = contentItemRepository.findById(saved.getId())
                .orElseThrow();

        assertThat(reloaded.getId()).isNotNull();
        assertThat(reloaded.getType()).isEqualTo(ContentType.ARTICLE);
        assertThat(reloaded.getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(reloaded.getPreferences()).containsExactlyInAnyOrder(
                SubscriberPreference.GENERAL_PREPAREDNESS,
                SubscriberPreference.PRACTICAL_SKILLS
        );
    }

    @Test
    void allowsContentWithoutPreferences() {
        ContentItem item = createContentItem(
                ContentType.VIDEO,
                ContentStatus.DRAFT
        );
        item.setYoutubeVideoId("dQw4w9WgXcQ");

        ContentItem saved = contentItemRepository.saveAndFlush(item);

        ContentItem reloaded = contentItemRepository.findById(saved.getId())
                .orElseThrow();

        assertThat(reloaded.getPreferences()).isEmpty();
        assertThat(reloaded.getYoutubeVideoId())
                .isEqualTo("dQw4w9WgXcQ");
    }

    @Test
    void filtersByTypeAndStatus() {
        ContentItem publishedVideo = createContentItem(
                ContentType.VIDEO,
                ContentStatus.PUBLISHED
        );
        publishedVideo.setYoutubeVideoId("dQw4w9WgXcQ");
        publishedVideo.setPublishedAt(LocalDateTime.now());

        ContentItem draftArticle = createContentItem(
                ContentType.ARTICLE,
                ContentStatus.DRAFT
        );

        contentItemRepository.saveAndFlush(publishedVideo);
        contentItemRepository.saveAndFlush(draftArticle);

        var page = contentItemRepository.findByTypeAndStatus(
                ContentType.VIDEO,
                ContentStatus.PUBLISHED,
                org.springframework.data.domain.PageRequest.of(0, 20)
        );

        assertThat(page.getContent())
                .filteredOn(item ->
                        item.getYoutubeVideoId() != null
                                && item.getYoutubeVideoId()
                                .equals("dQw4w9WgXcQ")
                )
                .hasSize(1);
    }

    private ContentItem createContentItem(
            ContentType type,
            ContentStatus status
    ) {
        LocalDateTime now = LocalDateTime.now();

        ContentItem item = new ContentItem();
        item.setType(type);
        item.setStatus(status);
        item.setTitle("Repository test content");
        item.setDescription("Repository test description");
        item.setCreatedAt(now);
        item.setUpdatedAt(now);

        return item;
    }
}
