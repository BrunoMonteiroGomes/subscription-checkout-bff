package com.globo.subscription.checkout.bff.service;

import com.globo.subscription.checkout.bff.client.PaymentMethodClient;
import com.globo.subscription.checkout.bff.client.ProductClient;
import com.globo.subscription.checkout.bff.dto.request.PreCheckoutRequest;
import com.globo.subscription.checkout.bff.dto.response.PreCheckoutResponse;
import com.globo.subscription.checkout.bff.exception.BusinessException;
import com.globo.subscription.checkout.bff.repository.CartRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class PreCheckoutService {

    private static final String CURRENCY = "BRL";

    private final ProductClient productClient;
    private final PaymentMethodClient paymentMethodClient;
    private final CartRepository cartRepository;

    public PreCheckoutService(ProductClient productClient,
                              PaymentMethodClient paymentMethodClient,
                              CartRepository cartRepository) {
        this.productClient = productClient;
        this.paymentMethodClient = paymentMethodClient;
        this.cartRepository = cartRepository;
    }

    public PreCheckoutResponse execute(PreCheckoutRequest request) {
        // 1. Resolve cada produto no serviço de produtos (fonte da verdade)
        List<ProductClient.ProductCatalogItem> resolved = request.products().stream()
                .map(ref -> {
                    ProductClient.ProductCatalogItem p = productClient.findBySlug(ref.slug());
                    if (p == null) {
                        throw BusinessException.productNotFound(ref.slug());
                    }
                    return p;
                })
                .toList();

        // 2. Busca os meios de pagamento no serviço de meios de pagamento
        List<PaymentMethodClient.PaymentMethodItem> methods = paymentMethodClient.listAvailable();

        // 3. Monta o carrinho
        BigDecimal total = resolved.stream()
                .map(ProductClient.ProductCatalogItem::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String cartId = UUID.randomUUID().toString();

        CartRepository.Cart cart = new CartRepository.Cart(
                cartId,
                resolved.stream()
                        .map(p -> new CartRepository.Cart.CartItem(p.slug(), p.name(), p.price()))
                        .toList(),
                methods.stream().map(PaymentMethodClient.PaymentMethodItem::name).toList(),
                CartRepository.Cart.Status.PENDING,
                total,
                CURRENCY,
                null // payment: ainda não houve cobrança
        );
        cartRepository.save(cart);

        // 4. Resposta só com o que a tela de pagamento precisa
        return new PreCheckoutResponse(
                cartId,
                resolved.stream()
                        .map(p -> new PreCheckoutResponse.ProductView(
                                p.slug(), p.name(), p.description(), p.price(), p.currency()))
                        .toList(),
                methods.stream()
                        .map(m -> new PreCheckoutResponse.PaymentMethodView(m.name()))
                        .toList()
        );
    }
}
