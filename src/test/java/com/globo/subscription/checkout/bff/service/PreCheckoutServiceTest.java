package com.globo.subscription.checkout.bff.service;

import com.globo.subscription.checkout.bff.client.PaymentMethodClient;
import com.globo.subscription.checkout.bff.client.ProductClient;
import com.globo.subscription.checkout.bff.dto.request.PreCheckoutRequest;
import com.globo.subscription.checkout.bff.exception.BusinessException;
import com.globo.subscription.checkout.bff.repository.CartRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreCheckoutServiceTest {

    @Mock
    ProductClient productClient;
    @Mock
    PaymentMethodClient paymentMethodClient;
    @Mock
    CartRepository cartRepository;
    @InjectMocks
    PreCheckoutService service;

    private static final ProductClient.ProductCatalogItem PREMIUM = new ProductClient.ProductCatalogItem(
            "premium", "Plano Premium", "Acesso completo", new BigDecimal("39.90"), "BRL");
    private static final ProductClient.ProductCatalogItem BASICO = new ProductClient.ProductCatalogItem(
            "basico", "Plano Básico", "Acesso básico", new BigDecimal("19.90"), "BRL");

    private static PreCheckoutRequest requestOf(String... slugs) {
        return new PreCheckoutRequest(
                java.util.Arrays.stream(slugs).map(PreCheckoutRequest.ProductRef::new).toList());
    }

    @Test
    void deveCriarCarrinhoPendenteERetornarProdutosEMeiosDePagamento() {
        when(productClient.findBySlug("premium")).thenReturn(PREMIUM);
        when(paymentMethodClient.listAvailable()).thenReturn(List.of(
                new PaymentMethodClient.PaymentMethodItem("Pix"),
                new PaymentMethodClient.PaymentMethodItem("Cartão de Crédito")));

        var response = service.execute(requestOf("premium"));

        assertThat(response.cartId()).isNotBlank();
        assertThat(response.products()).containsExactly(
                new com.globo.subscription.checkout.bff.dto.response.PreCheckoutResponse.ProductView(
                        "premium", "Plano Premium", "Acesso completo", new BigDecimal("39.90"), "BRL"));
        assertThat(response.paymentMethods()).extracting(m -> m.name())
                .containsExactly("Pix", "Cartão de Crédito");

        var captor = ArgumentCaptor.forClass(CartRepository.Cart.class);
        verify(cartRepository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.cartId()).isEqualTo(response.cartId());
        assertThat(saved.status()).isEqualTo(CartRepository.Cart.Status.PENDING);
        assertThat(saved.currency()).isEqualTo("BRL");
        assertThat(saved.total()).isEqualByComparingTo("39.90");
        assertThat(saved.payment()).isNull();
        assertThat(saved.availablePaymentMethods()).containsExactly("Pix", "Cartão de Crédito");
        assertThat(saved.items()).containsExactly(
                new CartRepository.Cart.CartItem("premium", "Plano Premium", new BigDecimal("39.90")));
    }

    @Test
    void deveSomarOTotalQuandoHouverVariosProdutos() {
        when(productClient.findBySlug("premium")).thenReturn(PREMIUM);
        when(productClient.findBySlug("basico")).thenReturn(BASICO);
        when(paymentMethodClient.listAvailable()).thenReturn(List.of(
                new PaymentMethodClient.PaymentMethodItem("Pix")));

        var response = service.execute(requestOf("premium", "basico"));

        assertThat(response.products()).extracting(p -> p.slug()).containsExactly("premium", "basico");

        var captor = ArgumentCaptor.forClass(CartRepository.Cart.class);
        verify(cartRepository).save(captor.capture());
        assertThat(captor.getValue().total()).isEqualByComparingTo("59.80");
        assertThat(captor.getValue().items()).hasSize(2);
    }

    @Test
    void deveGerarCartIdsDiferentesACadaPreCheckout() {
        when(productClient.findBySlug("premium")).thenReturn(PREMIUM);
        when(paymentMethodClient.listAvailable()).thenReturn(List.of());

        var first = service.execute(requestOf("premium"));
        var second = service.execute(requestOf("premium"));

        assertThat(first.cartId()).isNotEqualTo(second.cartId());
    }

    @Test
    void devePermitirListaDeMeiosDePagamentoVazia() {
        when(productClient.findBySlug("premium")).thenReturn(PREMIUM);
        when(paymentMethodClient.listAvailable()).thenReturn(List.of());

        var response = service.execute(requestOf("premium"));

        assertThat(response.paymentMethods()).isEmpty();
        verify(cartRepository).save(any());
    }

    @Test
    void deveFalharComProductNotFoundQuandoProdutoNaoExisteNoCatalogo() {
        when(productClient.findBySlug("inexistente")).thenReturn(null);

        assertThatThrownBy(() -> service.execute(requestOf("inexistente")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    var ex = (BusinessException) e;
                    assertThat(ex.code()).isEqualTo("PRODUCT_NOT_FOUND");
                    assertThat(ex.description()).contains("inexistente");
                });

        verifyNoInteractions(paymentMethodClient);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void deveFalharQuandoUmDosProdutosNaoExiste() {
        when(productClient.findBySlug("premium")).thenReturn(PREMIUM);
        when(productClient.findBySlug("inexistente")).thenReturn(null);

        assertThatThrownBy(() -> service.execute(requestOf("premium", "inexistente")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo("PRODUCT_NOT_FOUND");

        verify(cartRepository, never()).save(any());
    }
}
