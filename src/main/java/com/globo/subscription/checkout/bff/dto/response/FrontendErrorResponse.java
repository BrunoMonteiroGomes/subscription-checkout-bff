package com.globo.subscription.checkout.bff.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FrontendErrorResponse(
        String displayType,   // BLOCK | INTERVENTION
        String errorCode,     // código técnico (ex.: PAYMENT_DECLINED)
        String cartId,
        FrontendError frontendError
) {
    public record FrontendError(
            String code,        // ex.: P-1001
            String title,
            String description,
            Button primaryButton,
            Button secondaryButton
    ) {}

    public record Button(
            String label,
            String url
    ) {}
}
