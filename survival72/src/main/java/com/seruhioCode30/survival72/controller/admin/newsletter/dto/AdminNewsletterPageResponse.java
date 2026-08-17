package com.seruhioCode30.survival72.controller.admin.newsletter.dto;

import java.util.List;

public record AdminNewsletterPageResponse(
        List<AdminNewsletterResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
