package com.globo.subscription.checkout.bff.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PreCheckoutRequest(
        @NotEmpty @Valid List<ProductRef> products
) {
    public record ProductRef(
            @NotNull String slug
    ) {}
}
