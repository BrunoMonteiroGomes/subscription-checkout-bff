package com.globo.subscription.checkout.bff;

import org.springframework.boot.SpringApplication;

public class TestSubscriptionCheckoutBffApplication {

	public static void main(String[] args) {
		SpringApplication.from(SubscriptionCheckoutBffApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
