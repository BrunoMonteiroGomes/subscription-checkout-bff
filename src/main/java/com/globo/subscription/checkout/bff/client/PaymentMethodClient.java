package com.globo.subscription.checkout.bff.client;

import java.util.List;

public interface PaymentMethodClient {

    /**
     * Lista os meios de pagamento disponíveis no serviço de meios de pagamento.
     */
    List<PaymentMethodItem> listAvailable();

    record PaymentMethodItem(String name) {}
}
