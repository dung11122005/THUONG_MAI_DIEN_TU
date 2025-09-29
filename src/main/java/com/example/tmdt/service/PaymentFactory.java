package com.example.tmdt.service;

import java.util.List;

import org.springframework.stereotype.Service;


@Service
public class PaymentFactory {
    private final List<PaymentService> providers;

    public PaymentFactory(List<PaymentService> providers) {
        this.providers = providers;
    }

    public PaymentService getProvider(String name) {
        return providers.stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported provider: " + name));
    }
}
