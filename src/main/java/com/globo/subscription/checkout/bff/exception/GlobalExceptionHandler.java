package com.globo.subscription.checkout.bff.exception;

import com.globo.subscription.checkout.bff.dto.response.FrontendErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<FrontendErrorResponse> handleBusiness(BusinessException ex) {
        HttpStatus status = switch (ex.code()) {
            case "CART_NOT_FOUND", "PRODUCT_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "CART_ALREADY_APPROVED" -> HttpStatus.CONFLICT;
            case "PAYMENT_DECLINED" -> HttpStatus.PAYMENT_REQUIRED;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };

        FrontendErrorResponse body = toFrontendError(ex);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<FrontendErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .orElse("Requisição inválida.");

        FrontendErrorResponse body = new FrontendErrorResponse(
                "BLOCK",
                "VALIDATION_ERROR",
                null,
                new FrontendErrorResponse.FrontendError(
                        "P-0000",
                        "Dados inválidos",
                        msg,
                        new FrontendErrorResponse.Button("Tentar novamente", "RETRY"),
                        new FrontendErrorResponse.Button("Voltar", "/checkout/pagamento")
                )
        );
        return ResponseEntity.badRequest().body(body);
    }

    private FrontendErrorResponse toFrontendError(BusinessException ex) {
        String frontendCode = mapFrontendCode(ex.code());
        String displayType = ex.code().equals("CART_ALREADY_APPROVED") ? "INTERVENTION" : "BLOCK";

        FrontendErrorResponse.Button primary;
        FrontendErrorResponse.Button secondary;

        switch (ex.code()) {
            case "CART_ALREADY_APPROVED" -> {
                primary = new FrontendErrorResponse.Button("Ver comprovante", "/checkout/sucesso");
                secondary = null;
            }
            case "CART_NOT_FOUND" -> {
                primary = new FrontendErrorResponse.Button("Voltar ao início", "/checkout");
                secondary = null;
            }
            default -> {
                primary = new FrontendErrorResponse.Button("Tentar novamente", "RETRY");
                secondary = new FrontendErrorResponse.Button("Trocar meio de pagamento", "/checkout/pagamento");
            }
        }

        return new FrontendErrorResponse(
                displayType,
                ex.code(),
                null,
                new FrontendErrorResponse.FrontendError(
                        frontendCode,
                        ex.title(),
                        ex.description(),
                        primary,
                        secondary
                )
        );
    }

    private String mapFrontendCode(String backendCode) {
        return switch (backendCode) {
            case "PAYMENT_DECLINED"       -> "P-1001";
            case "CARD_NOT_FOUND"         -> "P-1002";
            case "CART_NOT_FOUND"         -> "P-1003";
            case "CART_ALREADY_APPROVED"  -> "P-1004";
            case "INVALID_PAYMENT_METHOD" -> "P-1005";
            case "CARD_REQUIRED"          -> "P-1006";
            case "INVALID_CARD"           -> "P-1007";
            default                       -> "P-9999";
        };
    }
}
