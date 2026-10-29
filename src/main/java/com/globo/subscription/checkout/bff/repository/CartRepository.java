package com.globo.subscription.checkout.bff.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CartRepository {

    void save(Cart cart);

    Optional<Cart> findById(String cartId);

    record Cart(
            String cartId,
            List<CartItem> items,
            List<String> availablePaymentMethods,
            Status status,
            BigDecimal total,
            String currency,
            PaymentInfo payment
    ) {
        public enum Status { PENDING, APPROVED, FAILED }

        public record CartItem(String slug, String name, BigDecimal price) {}

        public record PaymentInfo(
                String method,
                String lastDigits,   // para Pix, é o código Pix simulado
                BigDecimal amount,
                String currency,
                Instant paidAt
        ) {}

        public Cart withStatus(Status newStatus, PaymentInfo newPayment) {
            return new Cart(
                    cartId,
                    items,
                    availablePaymentMethods,
                    newStatus,
                    total,
                    currency,
                    newPayment
            );
        }
    }
}
