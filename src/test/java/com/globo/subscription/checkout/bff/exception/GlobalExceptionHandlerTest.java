package com.globo.subscription.checkout.bff.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @ParameterizedTest
    @CsvSource({
            "PRODUCT_NOT_FOUND,404,P-9999",
            "CART_NOT_FOUND,404,P-1003",
            "CART_ALREADY_APPROVED,409,P-1004",
            "PAYMENT_DECLINED,402,P-1001",
            "INVALID_PAYMENT_METHOD,422,P-1005",
            "CARD_REQUIRED,422,P-1006",
            "INVALID_CARD,422,P-1007"
    })
    void deveMapearCodigoDeNegocioParaStatusHttpECodigoDeFrontend(String code, int httpStatus, String frontendCode) {
        var ex = new BusinessException(code, "Título", "Descrição");

        var response = handler.handleBusiness(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(httpStatus);
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.errorCode()).isEqualTo(code);
        assertThat(body.frontendError().code()).isEqualTo(frontendCode);
        assertThat(body.frontendError().title()).isEqualTo("Título");
        assertThat(body.frontendError().description()).isEqualTo("Descrição");
    }

    @Test
    void deveRetornarPagamentoRecusadoComBotoesDeTentarNovamenteETrocarMeio() {
        var response = handler.handleBusiness(BusinessException.paymentDeclined());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
        var error = response.getBody().frontendError();
        assertThat(error.primaryButton().url()).isEqualTo("RETRY");
        assertThat(error.secondaryButton().url()).isEqualTo("/checkout/pagamento");
    }

    @Test
    void deveRetornarIntervencaoParaCarrinhoJaAprovado() {
        var response = handler.handleBusiness(BusinessException.cartAlreadyApproved());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        var body = response.getBody();
        assertThat(body.displayType()).isEqualTo("INTERVENTION");
        assertThat(body.frontendError().primaryButton().url()).isEqualTo("/checkout/sucesso");
        assertThat(body.frontendError().secondaryButton()).isNull();
    }
}
