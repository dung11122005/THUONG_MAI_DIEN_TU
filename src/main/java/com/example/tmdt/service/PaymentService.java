package com.example.tmdt.service;

import java.util.Map;

import com.example.tmdt.domain.dto.PaymentRequest;

import jakarta.servlet.http.HttpServletRequest;

public interface  PaymentService {
    String getName(); // "vnpay", "paypal", "momo"
    String createPaymentUrl(PaymentRequest request, HttpServletRequest servletRequest);
    boolean verifyReturn(Map<String, String> params);
}
