package com.seruhioCode30.survival72.controller.admin.newsletter.dto;

import java.util.List;

public record AdminNewsletterAudiencePreviewResponse(
        long totalAudience,
        List<AdminNewsletterAudienceMemberResponse> content,
        int page,
        int size,
        int totalPages,
        boolean hasNext
) {
}
