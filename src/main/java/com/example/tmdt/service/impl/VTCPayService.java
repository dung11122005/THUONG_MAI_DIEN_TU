package com.example.tmdt.service.impl;

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.tmdt.domain.dto.PaymentRequest;
import com.example.tmdt.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class VTCPayService implements PaymentService {

    @Value("${vtcpay.website.id}")
    private String websiteId;

    @Value("${vtcpay.secret.key}")
    private String secretKey;

    @Value("${vtcpay.mode}")
    private String mode; // "sandbox" hoặc "production"

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String getName() {
        return "VTCPAY";
    }

    @Override
    public String createPaymentUrl(PaymentRequest request, HttpServletRequest servletRequest) {
        try {
            // 1. Lấy base URL sandbox/production
            String baseUrl = "sandbox".equalsIgnoreCase(mode)
                    ? "https://alpha1.vtcpay.vn/portalgateway/checkout.html"
                    : "https://www.vtcpay.vn/portalgateway/checkout.html";

            // 2. Chuẩn bị tham số thanh toán
            String orderId = request.getOrderId();
            String amount = String.valueOf(request.getAmount()); // VND
            String returnUrl = getBaseUrl(servletRequest) + "/vtcpay-return?orderId=" + orderId;

            // 3. Tạo chuỗi chữ ký (signature) theo spec: website_id + orderId + amount + secretKey
            String rawSignature = websiteId + orderId + amount + secretKey;
            String signature = String.valueOf(rawSignature.hashCode()); // Demo, sandbox test

            // 4. Tạo URL thanh toán
            String paymentUrl = baseUrl
                    + "?website_id=" + URLEncoder.encode(websiteId, StandardCharsets.UTF_8)
                    + "&order_id=" + URLEncoder.encode(orderId, StandardCharsets.UTF_8)
                    + "&amount=" + URLEncoder.encode(amount, StandardCharsets.UTF_8)
                    + "&return_url=" + URLEncoder.encode(returnUrl, StandardCharsets.UTF_8)
                    + "&signature=" + URLEncoder.encode(signature, StandardCharsets.UTF_8);

            return paymentUrl;

        } catch (Exception e) {
            throw new RuntimeException("VTC Pay payment creation failed", e);
        }
    }

    @Override
    public boolean verifyReturn(Map<String, String> params) {
        try {
            String orderId = params.get("order_id");
            String amount = params.get("amount");
            String signature = params.get("signature");

            if (orderId == null || amount == null || signature == null) {
                return false;
            }

            // Tạo lại signature từ dữ liệu nhận được
            String rawSignature = websiteId + orderId + amount + secretKey;
            String expectedSignature = String.valueOf(rawSignature.hashCode()); // Demo sandbox

            return signature.equals(expectedSignature);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getBaseUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName()
                + ":" + request.getServerPort();
    }
}
