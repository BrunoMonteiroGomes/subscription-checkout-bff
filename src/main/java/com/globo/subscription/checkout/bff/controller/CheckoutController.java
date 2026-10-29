package com.globo.subscription.checkout.bff.controller;

import com.globo.subscription.checkout.bff.dto.request.CheckoutRequest;
import com.globo.subscription.checkout.bff.dto.response.CheckoutSuccessResponse;
import com.globo.subscription.checkout.bff.service.CheckoutService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/operations")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.OK)
    public CheckoutSuccessResponse checkout(@RequestBody @Valid CheckoutRequest request) {
        return checkoutService.execute(request);
    }
}
