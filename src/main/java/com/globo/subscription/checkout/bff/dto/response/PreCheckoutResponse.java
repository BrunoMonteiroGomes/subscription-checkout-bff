package com.globo.subscription.checkout.bff.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record PreCheckoutResponse(
        String cartId,
        List<ProductView> products,
        List<PaymentMethodView> paymentMethods
) {
    public record ProductView(
            String slug,
            String name,
            String description,
            BigDecimal price,
            String currency
    ) {}

    public record PaymentMethodView(String name) {}
}
