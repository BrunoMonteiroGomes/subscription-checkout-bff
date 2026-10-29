package com.globo.subscription.checkout.bff.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest (
    @NotBlank String cartId,
    @NotNull @Valid PaymentMethod paymentMethod
) {
        public record PaymentMethod(
                @NotBlank String name,
                @Valid Card card // obrigatório apenas para cartão; null para Pix
        ) {}

        public record Card(
                @NotBlank String holderName,
                @NotBlank String number,
                @NotBlank String expiry,
                @NotBlank String cvv
        ) {}
    }
