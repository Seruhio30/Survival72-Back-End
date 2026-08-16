package com.seruhioCode30.survival72.controller.admin.auth.dto;

public record AdminSessionResponse(
        boolean authenticated,
        String username,
        String csrfToken,
        String csrfHeaderName
) {
}
