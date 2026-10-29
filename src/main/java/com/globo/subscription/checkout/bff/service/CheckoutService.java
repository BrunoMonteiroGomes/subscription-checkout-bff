package com.globo.subscription.checkout.bff.service;

import com.globo.subscription.checkout.bff.dto.request.CheckoutRequest;
import com.globo.subscription.checkout.bff.dto.response.CheckoutSuccessResponse;
import com.globo.subscription.checkout.bff.exception.BusinessException;
import com.globo.subscription.checkout.bff.repository.CartRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class CheckoutService {

    private static final String PIX = "Pix";

    private final CartRepository cartRepository;

    public CheckoutService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public CheckoutSuccessResponse execute(CheckoutRequest request) {
        // 1. Carrinho precisa existir (foi criado no pré-checkout)
        CartRepository.Cart cart = cartRepository.findById(request.cartId())
                .orElseThrow(() -> BusinessException.cartNotFound(request.cartId()));

        // 2. Carrinho já aprovado não pode ser pago de novo
        if (cart.status() == CartRepository.Cart.Status.APPROVED) {
            throw BusinessException.cartAlreadyApproved();
        }

        // 3. Meio de pagamento precisa ser um dos devolvidos no pré-checkout daquele carrinho
        String chosenMethod = request.paymentMethod().name();
        if (!cart.availablePaymentMethods().contains(chosenMethod)) {
            throw BusinessException.invalidPaymentMethod(chosenMethod);
        }

        // 4. Regras específicas por meio
        PaymentResult result = chosenMethod.equals(PIX)
                ? processPix(cart)
                : processCard(cart, request.paymentMethod().card());

        // 5. Persiste status aprovado (apenas dados não sensíveis)
        CartRepository.Cart.PaymentInfo paymentInfo = new CartRepository.Cart.PaymentInfo(
                chosenMethod,
                result.lastDigitsOrPixCode(),
                result.amount(),
                cart.currency(),
                Instant.now()
        );
        CartRepository.Cart approved = cart.withStatus(
                CartRepository.Cart.Status.APPROVED,
                paymentInfo
        );
        cartRepository.save(approved);

        // 6. Resposta apenas com o que a tela de sucesso precisa
        return new CheckoutSuccessResponse(
                approved.cartId(),
                "APROVADO",
                approved.payment().paidAt(),
                new CheckoutSuccessResponse.Payment(
                        approved.payment().method(),
                        approved.payment().lastDigits(),
                        approved.payment().amount(),
                        approved.payment().currency()
                ),
                approved.items().stream()
                        .map(i -> new CheckoutSuccessResponse.ProductView(i.slug(), i.name(), i.price()))
                        .toList()
        );
    }

    // ---------- Cartão ----------

    private PaymentResult processCard(CartRepository.Cart cart, CheckoutRequest.Card card) {
        if (card == null) {
            throw BusinessException.cardRequired();
        }

        String digits = onlyDigits(card.number());
        if (digits.length() < 4) {
            throw BusinessException.invalidCard();
        }
        String lastDigits = digits.substring(digits.length() - 4);

        // Cartão terminado em 0002 é recusado (para demonstrar a tela de erro)
        if ("0002".equals(lastDigits)) {
            throw BusinessException.paymentDeclined();
        }

        // Simulação de cobrança: nenhum gateway real é chamado.
        return new PaymentResult(lastDigits, cart.total());
    }

    // ---------- Pix ----------

    private PaymentResult processPix(CartRepository.Cart cart) {
        // Simula a geração de um código Pix (não integra gateway real)
        String pixCode = "PIX-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        return new PaymentResult(pixCode, cart.total());
    }

    private static String onlyDigits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    /** Resultado interno da simulação de cobrança. */
    private record PaymentResult(String lastDigitsOrPixCode, BigDecimal amount) {}

}
