package com.globo.subscription.checkout.bff.repository;

import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryCartRepository implements CartRepository {

    private final ConcurrentMap<String, Cart> store = new ConcurrentHashMap<>();

    @Override
    public void save(Cart cart) {
        store.put(cart.cartId(), cart);
    }

    @Override
    public Optional<Cart> findById(String cartId) {
        return Optional.ofNullable(store.get(cartId));
    }
}
