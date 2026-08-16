package com.seruhioCode30.survival72.service.admin.content;

import com.seruhioCode30.survival72.controller.admin.content.dto.AdminContentCreateRequest;
import com.seruhioCode30.survival72.controller.admin.content.dto.AdminContentPageResponse;
import com.seruhioCode30.survival72.controller.admin.content.dto.AdminContentResponse;
import com.seruhioCode30.survival72.controller.admin.content.dto.AdminContentUpdateRequest;
import com.seruhioCode30.survival72.model.ContentItem;
import com.seruhioCode30.survival72.model.ContentStatus;
import com.seruhioCode30.survival72.model.ContentType;
import com.seruhioCode30.survival72.repository.ContentItemRepository;
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
public class AdminContentService {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private final ContentItemRepository contentItemRepository;
    private final YouTubeVideoIdNormalizer youTubeVideoIdNormalizer;

    public AdminContentService(
            ContentItemRepository contentItemRepository,
            YouTubeVideoIdNormalizer youTubeVideoIdNormalizer
    ) {
        this.contentItemRepository = contentItemRepository;
        this.youTubeVideoIdNormalizer = youTubeVideoIdNormalizer;
    }

    @Transactional(readOnly = true)
    public AdminContentPageResponse findContent(
            int page,
            int size,
            String typeValue,
            String statusValue
    ) {
        validatePagination(page, size);

        ContentType type = parseType(typeValue);
        ContentStatus status = parseStatus(statusValue);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<ContentItem> content = findPage(type, status, pageable);

        return new AdminContentPageResponse(
                content.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList(),
                content.getNumber(),
                content.getSize(),
                content.getTotalElements(),
                content.getTotalPages(),
                content.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public AdminContentResponse findById(Long id) {
        return toResponse(findEntity(id));
    }

    @Transactional
    public AdminContentResponse create(AdminContentCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();

        ContentItem contentItem = new ContentItem();
        contentItem.setType(request.type());
        contentItem.setTitle(normalizeRequiredTitle(request.title()));
        contentItem.setDescription(normalizeDescription(request.description()));
        contentItem.setStatus(
                request.status() == null
                        ? ContentStatus.DRAFT
                        : request.status()
        );
        contentItem.setPreferences(request.preferences());
        contentItem.setCreatedAt(now);
        contentItem.setUpdatedAt(now);

        applyMediaRules(
                contentItem,
                request.type(),
                request.youtubeVideoId()
        );
        applyPublicationRule(contentItem, now);

        return toResponse(contentItemRepository.save(contentItem));
    }

    @Transactional
    public AdminContentResponse update(
            Long id,
            AdminContentUpdateRequest request
    ) {
        ContentItem contentItem = findEntity(id);
        LocalDateTime now = LocalDateTime.now();

        ContentType resultingType = request.type() == null
                ? contentItem.getType()
                : request.type();

        if (request.title() != null) {
            contentItem.setTitle(normalizeRequiredTitle(request.title()));
        }

        if (request.description() != null) {
            contentItem.setDescription(
                    normalizeDescription(request.description())
            );
        }

        if (request.type() != null) {
            contentItem.setType(request.type());
        }

        if (request.status() != null) {
            contentItem.setStatus(request.status());
        }

        if (request.preferences() != null) {
            contentItem.setPreferences(request.preferences());
        }

        if (resultingType == ContentType.ARTICLE) {
            if (request.youtubeVideoId() != null
                    && !request.youtubeVideoId().isBlank()) {
                throw new IllegalArgumentException(
                        "ARTICLE cannot use YouTube video."
                );
            }

            contentItem.setYoutubeVideoId(null);
        } else if (request.youtubeVideoId() != null) {
            contentItem.setYoutubeVideoId(
                    youTubeVideoIdNormalizer.normalize(
                            request.youtubeVideoId()
                    )
            );
        } else if (contentItem.getYoutubeVideoId() == null) {
            throw new IllegalArgumentException(
                    "VIDEO requires YouTube video."
            );
        }

        contentItem.setUpdatedAt(now);
        applyPublicationRule(contentItem, now);

        return toResponse(contentItemRepository.save(contentItem));
    }

    private void applyMediaRules(
            ContentItem contentItem,
            ContentType type,
            String youtubeValue
    ) {
        if (type == ContentType.ARTICLE) {
            if (youtubeValue != null && !youtubeValue.isBlank()) {
                throw new IllegalArgumentException(
                        "ARTICLE cannot use YouTube video."
                );
            }

            contentItem.setYoutubeVideoId(null);
            return;
        }

        contentItem.setYoutubeVideoId(
                youTubeVideoIdNormalizer.normalize(youtubeValue)
        );
    }

    private void applyPublicationRule(
            ContentItem contentItem,
            LocalDateTime now
    ) {
        if (contentItem.getStatus() == ContentStatus.PUBLISHED
                && contentItem.getPublishedAt() == null) {
            contentItem.setPublishedAt(now);
        }
    }

    private ContentItem findEntity(Long id) {
        return contentItemRepository.findById(id)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Content item not found."
                        )
                );
    }

    private Page<ContentItem> findPage(
            ContentType type,
            ContentStatus status,
            Pageable pageable
    ) {
        if (type != null && status != null) {
            return contentItemRepository.findByTypeAndStatus(
                    type,
                    status,
                    pageable
            );
        }

        if (type != null) {
            return contentItemRepository.findByType(type, pageable);
        }

        if (status != null) {
            return contentItemRepository.findByStatus(status, pageable);
        }

        return contentItemRepository.findAll(pageable);
    }

    private AdminContentResponse toResponse(ContentItem contentItem) {
        return new AdminContentResponse(
                contentItem.getId(),
                contentItem.getType(),
                contentItem.getStatus(),
                contentItem.getTitle(),
                contentItem.getDescription(),
                contentItem.getYoutubeVideoId(),
                new LinkedHashSet<>(contentItem.getPreferences()),
                contentItem.getPublishedAt(),
                contentItem.getCreatedAt(),
                contentItem.getUpdatedAt()
        );
    }

    private String normalizeRequiredTitle(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Title is required.");
        }

        return value.trim();
    }

    private String normalizeDescription(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Invalid page.");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Invalid size.");
        }
    }

    private ContentType parseType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return ContentType.valueOf(
                    value.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid type.");
        }
    }

    private ContentStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return ContentStatus.valueOf(
                    value.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid status.");
        }
    }
}
