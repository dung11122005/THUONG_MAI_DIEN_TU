package com.example.tmdt.service.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.tmdt.domain.dto.PaymentRequest;
import com.example.tmdt.service.PaymentService;

import jakarta.servlet.http.HttpServletRequest;


@Service
public class PayPalService implements PaymentService{
    @Value("${paypal.client.id}")
    private String clientId;

    @Value("${paypal.client.secret}")
    private String clientSecret;

    @Value("${paypal.mode}")
    private String mode;

    @Override
    public String getName() {
        return "PAYPAL";
    }

    @Override
    public String createPaymentUrl(PaymentRequest request, HttpServletRequest servletRequest) {
        try {
            // 1. Tạo APIContext
            com.paypal.base.rest.APIContext apiContext =
                    new com.paypal.base.rest.APIContext(clientId, clientSecret, mode);

            // 2. Định nghĩa payment amount
            com.paypal.api.payments.Amount amount = new com.paypal.api.payments.Amount();
            amount.setCurrency("USD");
            amount.setTotal(String.format("%.2f", request.getAmount() / 24000)); 
            // giả sử bạn lưu VNĐ, convert qua USD

            // 3. Transaction
            com.paypal.api.payments.Transaction transaction = new com.paypal.api.payments.Transaction();
            transaction.setDescription("Thanh toán đơn hàng #" + request.getOrderId());
            transaction.setAmount(amount);

            java.util.List<com.paypal.api.payments.Transaction> transactions = new java.util.ArrayList<>();
            transactions.add(transaction);

            // 4. Payer
            com.paypal.api.payments.Payer payer = new com.paypal.api.payments.Payer();
            payer.setPaymentMethod("paypal");

            // 5. Redirect URLs
            com.paypal.api.payments.RedirectUrls redirectUrls = new com.paypal.api.payments.RedirectUrls();
            redirectUrls.setCancelUrl(getBaseUrl(servletRequest) + "/thank/paypal?success=false&orderId=" + request.getOrderId());
            redirectUrls.setReturnUrl(getBaseUrl(servletRequest) + "/thank/paypal?success=true&orderId=" + request.getOrderId());

            // 6. Payment object
            com.paypal.api.payments.Payment payment = new com.paypal.api.payments.Payment();
            payment.setIntent("sale");
            payment.setPayer(payer);
            payment.setTransactions(transactions);
            payment.setRedirectUrls(redirectUrls);

            // 7. Create payment
            com.paypal.api.payments.Payment createdPayment = payment.create(apiContext);

            // 8. Lấy approval URL để redirect
            for (com.paypal.api.payments.Links link : createdPayment.getLinks()) {
                if ("approval_url".equalsIgnoreCase(link.getRel())) {
                    return link.getHref();
                }
            }
            throw new RuntimeException("Không tìm thấy PayPal approval URL");
        } catch (Exception e) {
            throw new RuntimeException("PayPal payment creation failed", e);
        }
    }

    @Override
    public boolean verifyReturn(Map<String, String> params) {
        try {
            String paymentId = params.get("paymentId");
            String payerId = params.get("PayerID");

            com.paypal.base.rest.APIContext apiContext =
                    new com.paypal.base.rest.APIContext(clientId, clientSecret, mode);

            com.paypal.api.payments.Payment payment =
                    com.paypal.api.payments.Payment.get(apiContext, paymentId);

            com.paypal.api.payments.PaymentExecution paymentExecution =
                    new com.paypal.api.payments.PaymentExecution();
            paymentExecution.setPayerId(payerId);

            payment.execute(apiContext, paymentExecution);

            return true; // thành công
        } catch (Exception e) {
            e.printStackTrace();
            return false; // thất bại
        }
    }

    private String getBaseUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
    }
}
