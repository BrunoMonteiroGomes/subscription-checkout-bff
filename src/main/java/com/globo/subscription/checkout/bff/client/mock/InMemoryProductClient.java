package com.globo.subscription.checkout.bff.client.mock;

import com.globo.subscription.checkout.bff.client.ProductClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Simula o serviço externo de produtos.
 * O catálogo é fixo AQUI (lado do serviço), não no controller do BFF.
 */

@Component
public class InMemoryProductClient implements ProductClient {

    private static final Map<String, ProductCatalogItem> CATALOG = Map.of(
            "basico", new ProductCatalogItem(
                    "basico",
                    "Plano Básico",
                    "Assinatura mensal com acesso essencial",
                    new BigDecimal("19.90"),
                    "BRL"),
            "premium", new ProductCatalogItem(
                    "premium",
                    "Plano Premium",
                    "Assinatura mensal com catálogo completo",
                    new BigDecimal("39.90"),
                    "BRL"),
            "familia", new ProductCatalogItem(
                    "familia",
                    "Plano Família",
                    "Assinatura mensal para até 5 perfis",
                    new BigDecimal("59.90"),
                    "BRL")
    );

    @Override
    public ProductCatalogItem findBySlug(String slug) {
        return CATALOG.get(slug); // null => produto inexistente;
    }
}
