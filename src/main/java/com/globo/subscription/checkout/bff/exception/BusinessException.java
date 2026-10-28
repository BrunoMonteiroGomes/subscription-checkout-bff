package com.globo.subscription.checkout.bff.exception;

public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static BusinessException productNotFound(String slug) {
        return new BusinessException("PRODUCT_NOT_FOUND",
                "Produto inexistente no pré-checkout: " + slug);
    }
}
