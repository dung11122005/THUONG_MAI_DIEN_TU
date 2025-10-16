package com.example.tmdt.service.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.tmdt.domain.dto.PaymentRequest;
import com.example.tmdt.service.PaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class PayPalService implements PaymentService {
    @Value("${paypal.client.id}")
    private String clientId;

    @Value("${paypal.client.secret}")
    private String clientSecret;

    @Value("${paypal.mode}")
    private String mode;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String getName() {
        return "PAYPAL";
    }

    @Override
    public String createPaymentUrl(PaymentRequest request, HttpServletRequest servletRequest) {
        try {
            // 1. Lấy access token
            String accessToken = getPayPalAccessToken();
            
            // 2. Xác định base URL dựa vào mode
            String baseApiUrl = "sandbox".equalsIgnoreCase(mode) 
                ? "https://api-m.sandbox.paypal.com" 
                : "https://api-m.paypal.com";
            
            // 3. Chuẩn bị request body
            ObjectNode rootNode = objectMapper.createObjectNode();
            rootNode.put("intent", "CAPTURE");
            
            ArrayNode purchaseUnits = rootNode.putArray("purchase_units");
            ObjectNode purchaseUnit = purchaseUnits.addObject();
            
            ObjectNode amount = purchaseUnit.putObject("amount");
            amount.put("currency_code", "USD");
            amount.put("value", String.format("%.2f", request.getAmount() / 24000));
            
            purchaseUnit.put("description", "Thanh toán đơn hàng #" + request.getOrderId());
            
            // 4. Thêm application_context để chỉ định return URL và cancel URL
            ObjectNode applicationContext = rootNode.putObject("application_context");
            applicationContext.put("return_url", getBaseUrl(servletRequest) + "/thank/paypal?success=true&orderId=" + request.getOrderId());
            applicationContext.put("cancel_url", getBaseUrl(servletRequest) + "/thank/paypal?success=false&orderId=" + request.getOrderId());
            
            // 5. Gửi request để tạo order
            HttpRequest paypalRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseApiUrl + "/v2/checkout/orders"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(rootNode.toString()))
                .build();
                
            HttpResponse<String> response = httpClient.send(paypalRequest, 
                HttpResponse.BodyHandlers.ofString());
                
            // 6. Xử lý response
            JsonNode responseJson = objectMapper.readTree(response.body());
            
            if (response.statusCode() != 201) {
                throw new RuntimeException("PayPal API error: " + responseJson.toString());
            }
            
            // 7. Lấy approval URL từ response
            JsonNode links = responseJson.get("links");
            for (JsonNode link : links) {
                if ("approve".equals(link.get("rel").asText())) {
                    return link.get("href").asText();
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
            // 1. Lấy order ID và thông tin thanh toán
            String orderId = params.get("token");
            
            if (orderId == null) {
                return false;
            }
            
            // 2. Lấy access token
            String accessToken = getPayPalAccessToken();
            
            // 3. Xác định base URL dựa vào mode
            String baseApiUrl = "sandbox".equalsIgnoreCase(mode) 
                ? "https://api-m.sandbox.paypal.com" 
                : "https://api-m.paypal.com";
                
            // 4. Gửi request để capture payment
            ObjectNode captureNode = objectMapper.createObjectNode();
            
            HttpRequest captureRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseApiUrl + "/v2/checkout/orders/" + orderId + "/capture"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(captureNode.toString()))
                .build();
                
            HttpResponse<String> response = httpClient.send(captureRequest, 
                HttpResponse.BodyHandlers.ofString());
            
            // 5. Kiểm tra kết quả
            return response.statusCode() == 201;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private String getPayPalAccessToken() throws Exception {
        // Xác định base URL dựa vào mode
        String baseTokenUrl = "sandbox".equalsIgnoreCase(mode) 
            ? "https://api-m.sandbox.paypal.com/v1/oauth2/token" 
            : "https://api-m.paypal.com/v1/oauth2/token";
            
        // Tạo Basic Authentication header từ client ID và secret
        String auth = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
        
        // Gửi request để lấy access token
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseTokenUrl))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Authorization", "Basic " + auth)
            .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
            .build();
            
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());
            
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to get PayPal access token: " + response.body());
        }
        
        JsonNode responseJson = objectMapper.readTree(response.body());
        return responseJson.get("access_token").asText();
    }

    private String getBaseUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
    }
}