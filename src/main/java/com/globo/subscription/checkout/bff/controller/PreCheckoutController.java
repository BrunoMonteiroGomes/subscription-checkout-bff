package com.globo.subscription.checkout.bff.controller;

import com.globo.subscription.checkout.bff.dto.request.PreCheckoutRequest;
import com.globo.subscription.checkout.bff.dto.response.PreCheckoutResponse;
import com.globo.subscription.checkout.bff.service.PreCheckoutService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/operations")
public class PreCheckoutController {

    private final PreCheckoutService preCheckoutService;

    public PreCheckoutController(PreCheckoutService preCheckoutService) {
        this.preCheckoutService = preCheckoutService;
    }

    @PostMapping("/pre-checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public PreCheckoutResponse preCheckout(@RequestBody @Valid PreCheckoutRequest request) {
        return preCheckoutService.execute(request);
    }
}
