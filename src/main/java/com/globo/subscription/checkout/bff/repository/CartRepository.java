package com.globo.subscription.checkout.bff.repository;

import java.math.BigDecimal;
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
            String currency
    ) {
        public enum Status { PENDING, APPROVED, FAILED }

        public record CartItem(String slug, String name, BigDecimal price) {}
    }
}
