package com.globo.subscription.checkout.bff.service;

import com.globo.subscription.checkout.bff.dto.request.CheckoutRequest;
import com.globo.subscription.checkout.bff.exception.BusinessException;
import com.globo.subscription.checkout.bff.repository.CartRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CheckoutServiceTest {

    @Mock
    CartRepository cartRepository;
    @InjectMocks
    CheckoutService service;

    @Test
    void deveRecusarCartaoTerminadoEm0002() {
        var cart = new CartRepository.Cart(
                "cart-1",
                List.of(new CartRepository.Cart.CartItem("premium", "Plano Premium", new BigDecimal("39.90"))),
                List.of("Cartão de Crédito"),
                CartRepository.Cart.Status.PENDING,
                new BigDecimal("39.90"),
                "BRL",
                null);
        when(cartRepository.findById("cart-1")).thenReturn(Optional.of(cart));

        var req = new CheckoutRequest("cart-1",
                new CheckoutRequest.PaymentMethod("Cartão de Crédito",
                        new CheckoutRequest.Card("Ana", "4111111111110002", "12/28", "123")));

        assertThatThrownBy(() -> service.execute(req))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo("PAYMENT_DECLINED");

        verify(cartRepository, never()).save(any());
    }

    // ---------- Sucesso ----------

    @Test
    void deveAprovarPagamentoComCartaoEPersistirApenasUltimosQuatroDigitos() {
        when(cartRepository.findById("cart-1")).thenReturn(Optional.of(pendingCart("Pix", "Cartão de Crédito")));

        var response = service.execute(cardRequest("4111 1111 1111 1111"));

        assertThat(response.cartId()).isEqualTo("cart-1");
        assertThat(response.status()).isEqualTo("APROVADO");
        assertThat(response.paidAt()).isNotNull();
        assertThat(response.payment().method()).isEqualTo("Cartão de Crédito");
        assertThat(response.payment().lastDigits()).isEqualTo("1111");
        assertThat(response.payment().amount()).isEqualByComparingTo("39.90");
        assertThat(response.payment().currency()).isEqualTo("BRL");
        assertThat(response.products()).extracting(p -> p.slug()).containsExactly("premium");

        var captor = ArgumentCaptor.forClass(CartRepository.Cart.class);
        verify(cartRepository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.status()).isEqualTo(CartRepository.Cart.Status.APPROVED);
        assertThat(saved.payment().lastDigits()).isEqualTo("1111");
        assertThat(saved.payment().paidAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void deveAprovarPagamentoComPixGerandoCodigoSimulado() {
        when(cartRepository.findById("cart-1")).thenReturn(Optional.of(pendingCart("Pix", "Cartão de Crédito")));

        var req = new CheckoutRequest("cart-1", new CheckoutRequest.PaymentMethod("Pix", null));
        var response = service.execute(req);

        assertThat(response.status()).isEqualTo("APROVADO");
        assertThat(response.payment().method()).isEqualTo("Pix");
        assertThat(response.payment().lastDigits()).startsWith("PIX-").hasSize(16);
        assertThat(response.payment().amount()).isEqualByComparingTo("39.90");

        var captor = ArgumentCaptor.forClass(CartRepository.Cart.class);
        verify(cartRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(CartRepository.Cart.Status.APPROVED);
        assertThat(captor.getValue().payment().lastDigits()).isEqualTo(response.payment().lastDigits());
    }

    @Test
    void deveAprovarPixIgnorandoDadosDeCartaoEnviados() {
        when(cartRepository.findById("cart-1")).thenReturn(Optional.of(pendingCart("Pix")));

        var req = new CheckoutRequest("cart-1", new CheckoutRequest.PaymentMethod("Pix",
                new CheckoutRequest.Card("Ana", "4111111111110002", "12/28", "123")));
        var response = service.execute(req);

        assertThat(response.payment().lastDigits()).startsWith("PIX-");
    }

    // ---------- Erros ----------

    @Test
    void deveFalharQuandoCarrinhoNaoExiste() {
        when(cartRepository.findById("cart-1")).thenReturn(Optional.empty());

        assertBusinessError(cardRequest("4111111111111111"), "CART_NOT_FOUND");
        verify(cartRepository, never()).save(any());
    }

    @Test
    void deveFalharQuandoCarrinhoJaFoiAprovado() {
        var approved = pendingCart("Cartão de Crédito")
                .withStatus(CartRepository.Cart.Status.APPROVED, null);
        when(cartRepository.findById("cart-1")).thenReturn(Optional.of(approved));

        assertBusinessError(cardRequest("4111111111111111"), "CART_ALREADY_APPROVED");
        verify(cartRepository, never()).save(any());
    }

    @Test
    void deveFalharQuandoMeioDePagamentoNaoEstaDisponivelNoCarrinho() {
        when(cartRepository.findById("cart-1")).thenReturn(Optional.of(pendingCart("Cartão de Crédito")));

        var req = new CheckoutRequest("cart-1", new CheckoutRequest.PaymentMethod("Pix", null));

        assertBusinessError(req, "INVALID_PAYMENT_METHOD");
        verify(cartRepository, never()).save(any());
    }

    @Test
    void deveFalharQuandoCartaoNaoForInformadoParaMeioCartao() {
        when(cartRepository.findById("cart-1")).thenReturn(Optional.of(pendingCart("Cartão de Crédito")));

        var req = new CheckoutRequest("cart-1", new CheckoutRequest.PaymentMethod("Cartão de Crédito", null));

        assertBusinessError(req, "CARD_REQUIRED");
        verify(cartRepository, never()).save(any());
    }

    @Test
    void deveFalharQuandoNumeroDoCartaoTemMenosDeQuatroDigitos() {
        when(cartRepository.findById("cart-1")).thenReturn(Optional.of(pendingCart("Cartão de Crédito")));

        assertBusinessError(cardRequest("12-3"), "INVALID_CARD");
        verify(cartRepository, never()).save(any());
    }

    @Test
    void deveRecusarCartaoTerminadoEm0002MesmoComMascara() {
        when(cartRepository.findById("cart-1")).thenReturn(Optional.of(pendingCart("Cartão de Crédito")));

        assertBusinessError(cardRequest("4111-1111-1111-0002"), "PAYMENT_DECLINED");
        verify(cartRepository, never()).save(any());
    }

    // ---------- Helpers ----------

    private static CartRepository.Cart pendingCart(String... methods) {
        return new CartRepository.Cart(
                "cart-1",
                List.of(new CartRepository.Cart.CartItem("premium", "Plano Premium", new BigDecimal("39.90"))),
                List.of(methods),
                CartRepository.Cart.Status.PENDING,
                new BigDecimal("39.90"),
                "BRL",
                null);
    }

    private static CheckoutRequest cardRequest(String number) {
        return new CheckoutRequest("cart-1",
                new CheckoutRequest.PaymentMethod("Cartão de Crédito",
                        new CheckoutRequest.Card("Ana", number, "12/28", "123")));
    }

    private void assertBusinessError(CheckoutRequest req, String expectedCode) {
        assertThatThrownBy(() -> service.execute(req))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(expectedCode);
    }
}
