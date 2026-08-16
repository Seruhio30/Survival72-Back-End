package com.seruhioCode30.survival72.controller.admin.subscriber.dto;

import java.util.List;

public record AdminSubscriberPageResponse(
        List<AdminSubscriberResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
