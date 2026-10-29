package com.globo.subscription.checkout.bff.client.mock;

import com.globo.subscription.checkout.bff.client.PaymentMethodClient;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * Simula o serviço externo de meios de pagamento.
 * Os três meios vêm DAQUI, não de uma lista fixa no controller do BFF.
 */

@Component
public class InMemoryPaymentMethodClient implements PaymentMethodClient {

    @Override
    public List<PaymentMethodItem> listAvailable() {
        return List.of(
                new PaymentMethodItem("Cartão de Crédito"),
                new PaymentMethodItem("Cartão de Débito"),
                new PaymentMethodItem("Pix")
        );
    }
}
