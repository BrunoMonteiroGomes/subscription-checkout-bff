package com.globo.subscription.checkout.bff.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CheckoutSuccessResponse(
        String cartId,
        String status,
        Instant paidAt,
        Payment payment,
        List<ProductView> products
) {
    public record Payment(
            String method,
            String lastDigits, // para Pix, vira o código Pix simulado
            BigDecimal amount,
            String currency
    ) {}

    public record ProductView(
            String slug,
            String name,
            BigDecimal price
    ) {}
}
