package com.seruhioCode30.survival72.controller.admin.content.dto;

import com.seruhioCode30.survival72.model.ContentStatus;
import com.seruhioCode30.survival72.model.ContentType;
import com.seruhioCode30.survival72.model.SubscriberPreference;

import java.time.LocalDateTime;
import java.util.Set;

public record AdminContentResponse(
        Long id,
        ContentType type,
        ContentStatus status,
        String title,
        String description,
        String youtubeVideoId,
        Set<SubscriberPreference> preferences,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
