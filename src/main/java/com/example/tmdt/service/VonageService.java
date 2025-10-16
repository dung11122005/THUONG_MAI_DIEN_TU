package com.example.tmdt.service;

import com.vonage.client.VonageClient;
import com.vonage.client.sms.MessageStatus;
import com.vonage.client.sms.SmsSubmissionResponse;
import com.vonage.client.sms.messages.TextMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class VonageService {

    @Value("${vonage.api.key}")
    private String apiKey;

    @Value("${vonage.api.secret}")
    private String apiSecret;

    @Value("${vonage.brand.name:VERIFY}")
    private String brandName;

    private VonageClient client;

    private void initializeClient() {
        if (client == null) {
            client = VonageClient.builder()
                    .apiKey(apiKey)
                    .apiSecret(apiSecret)
                    .build();
        }
    }

    public boolean sendOtp(String phoneNumber, String otpCode) {
        try {
            // Khởi tạo client nếu chưa
            initializeClient();

            // Chuẩn hóa số điện thoại
            if (phoneNumber.startsWith("0")) {
                phoneNumber = "84" + phoneNumber.substring(1);
            } else if (phoneNumber.startsWith("+84")) {
                phoneNumber = phoneNumber.substring(1); // Xóa dấu +
            } else if (!phoneNumber.startsWith("84")) {
                phoneNumber = "84" + phoneNumber;
            }

            System.out.println("Sending OTP via Vonage to: " + phoneNumber);
            System.out.println("Message: Ma xac thuc OTP cua ban la: " + otpCode);

            // Gửi SMS
            TextMessage message = new TextMessage(
                    brandName,
                    phoneNumber,
                    "Ma xac thuc OTP cua ban la: " + otpCode
            );

            SmsSubmissionResponse response = client.getSmsClient().submitMessage(message);

            // Kiểm tra kết quả
            System.out.println("SMS API Response: " + response);
            
            if (response.getMessages().isEmpty()) {
                System.err.println("No message returned in response");
                return false;
            }
            
            // In ra chi tiết status
            response.getMessages().forEach(msg -> {
                System.out.println("Status: " + msg.getStatus());
                if (msg.getErrorText() != null) {
                    System.out.println("Error text: " + msg.getErrorText());
                }
            });
            
            // Kiểm tra trạng thái gửi
            return response.getMessages().get(0).getStatus() == MessageStatus.OK;
            
        } catch (Exception e) {
            System.err.println("Error sending SMS via Vonage: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Kiểm tra số dư tài khoản
    public String checkBalance() {
        try {
            initializeClient();
            return "Đã gửi yêu cầu SMS. Kiểm tra số dư trong bảng điều khiển Vonage (dashboard.nexmo.com)";
        } catch (Exception e) {
            System.err.println("Error checking balance: " + e.getMessage());
            return "Lỗi kết nối Vonage: " + e.getMessage();
        }
    }

    // Tạo mã OTP ngẫu nhiên
    public String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // 6 chữ số
        return String.valueOf(otp);
    }
}