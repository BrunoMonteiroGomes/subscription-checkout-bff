package com.globo.subscription.checkout.bff.exception;

public class BusinessException extends RuntimeException {

    private final String code;
    private final String title;
    private final String description;

    public BusinessException(String code, String title, String description) {
        super(description);
        this.code = code;
        this.title = title;
        this.description = description;
    }

    public String code() {
        return code;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    // ---- Pré-checkout ----
    public static BusinessException productNotFound(String slug) {
        return new BusinessException(
                "PRODUCT_NOT_FOUND",
                "Produto indisponível",
                "O produto '" + slug + "' não existe no catálogo.");
    }

    // ---- Checkout ----
    public static BusinessException cartNotFound(String cartId) {
        return new BusinessException(
                "CART_NOT_FOUND",
                "Carrinho não encontrado",
                "Não encontramos o carrinho informado. Inicie um novo pré-checkout.");
    }

    public static BusinessException cartAlreadyApproved() {
        return new BusinessException(
                "CART_ALREADY_APPROVED",
                "Pagamento já aprovado",
                "Este carrinho já foi pago e não pode ser cobrado novamente.");
    }

    public static BusinessException invalidPaymentMethod(String method) {
        return new BusinessException(
                "INVALID_PAYMENT_METHOD",
                "Meio de pagamento inválido",
                "O meio '" + method + "' não está disponível para este carrinho.");
    }

    public static BusinessException cardRequired() {
        return new BusinessException(
                "CARD_REQUIRED",
                "Dados do cartão ausentes",
                "Informe os dados do cartão para este meio de pagamento.");
    }

    public static BusinessException invalidCard() {
        return new BusinessException(
                "INVALID_CARD",
                "Cartão inválido",
                "Confira os dados do cartão e tente novamente.");
    }

    public static BusinessException paymentDeclined() {
        return new BusinessException(
                "PAYMENT_DECLINED",
                "Pagamento não aprovado",
                "Não foi possível concluir o pagamento. Tente novamente ou escolha outro meio.");
    }
}
