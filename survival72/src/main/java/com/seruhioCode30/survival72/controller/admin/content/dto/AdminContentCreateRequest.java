package com.seruhioCode30.survival72.controller.admin.content.dto;

import com.seruhioCode30.survival72.model.ContentStatus;
import com.seruhioCode30.survival72.model.ContentType;
import com.seruhioCode30.survival72.model.SubscriberPreference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record AdminContentCreateRequest(
        @NotNull(message = "type is required")
        ContentType type,

        @NotBlank(message = "title is required")
        @Size(max = 200, message = "title must not exceed 200 characters")
        String title,

        @Size(max = 2000, message = "description must not exceed 2000 characters")
        String description,

        @Size(max = 255, message = "youtubeVideoId must not exceed 255 characters")
        String youtubeVideoId,

        ContentStatus status,

        Set<SubscriberPreference> preferences
) {
}
