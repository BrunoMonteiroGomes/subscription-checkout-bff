package com.globo.subscription.checkout.bff.client;

import java.math.BigDecimal;

public interface ProductClient {

    /**
     * Busca um produto no catálogo do serviço de produtos.
     * @return o produto ou {@code null} se não existir no catálogo.
     */
    ProductCatalogItem findBySlug(String slug);

    record ProductCatalogItem(
            String slug,
            String name,
            String description,
            BigDecimal price,
            String currency
    ) {}
}
