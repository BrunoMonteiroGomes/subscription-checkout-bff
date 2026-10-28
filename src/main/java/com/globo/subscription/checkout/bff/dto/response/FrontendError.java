package com.globo.subscription.checkout.bff.dto.response;

import java.time.Instant;

public record FrontendError(
        String code,
        String message,
        Instant timestamp
) {
    public static FrontendError of(String code, String message) {
        return new FrontendError(code, message, Instant.now());
    }
}
