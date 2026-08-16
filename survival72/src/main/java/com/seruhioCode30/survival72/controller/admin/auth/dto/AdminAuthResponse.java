package com.seruhioCode30.survival72.controller.admin.auth.dto;

public record AdminAuthResponse(
        String status,
        String message,
        boolean authenticated
) {
}
