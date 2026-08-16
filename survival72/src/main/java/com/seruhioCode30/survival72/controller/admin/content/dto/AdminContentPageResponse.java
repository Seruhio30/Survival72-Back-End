package com.seruhioCode30.survival72.controller.admin.content.dto;

import java.util.List;

public record AdminContentPageResponse(
        List<AdminContentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
