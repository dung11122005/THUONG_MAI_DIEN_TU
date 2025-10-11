package com.example.tmdt.service;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;

import jakarta.annotation.PostConstruct;

@Service
public class OtpService {

    @Value("${twilio.account_sid}")
    private String accountSid;

    @Value("${twilio.auth_token}")
    private String authToken;

    @Value("${twilio.service_sid}")
    private String serviceSid;

    @Autowired
    private JavaMailSender mailSender;

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
    }

    // trả về true nếu gửi thành công, false nếu thất bại (log lỗi)
    public boolean sendOtp(String phoneNumber) {
        String to = normalizePhone(phoneNumber);
        if (to == null) {
            System.err.println("OtpService.sendOtp: invalid phone -> " + phoneNumber);
            return false;
        }

        try {
            Verification verification = Verification.creator(
                    serviceSid,
                    to,
                    "sms"
            ).create();
            System.out.println("OTP sent to: " + to + " | Status: " + verification.getStatus());
            return true;
        } catch (ApiException apiEx) {
            // Twilio returned an error (e.g. trial unverified number)
            System.err.println("Twilio API error sending OTP to " + to + " : " + apiEx.getMessage());
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean verifyOtp(String phoneNumber, String code) {
        String to = normalizePhone(phoneNumber);
        if (to == null) {
            System.err.println("OtpService.verifyOtp: invalid phone -> " + phoneNumber);
            return false;
        }

        try {
            VerificationCheck verificationCheck = VerificationCheck.creator(serviceSid)
                    .setTo(to)
                    .setCode(code)
                    .create();

            return "approved".equalsIgnoreCase(verificationCheck.getStatus());
        } catch (ApiException apiEx) {
            System.err.println("Twilio API error verifying OTP for " + to + " : " + apiEx.getMessage());
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        String p = phone.replaceAll("\\s+", "").replaceAll("[^+0-9]", "");
        if (p.isEmpty()) return null;
        if (p.startsWith("+")) return p;
        if (p.startsWith("0")) {
            return "+84" + p.substring(1);
        }
        if (p.startsWith("84")) {
            return "+" + p;
        }
        if (p.matches("\\d{8,15}")) {
            return p;
        }
        return null;
    }

    public String generateDevOtp() {
        Random r = new Random();
        int num = 100000 + r.nextInt(900000);
        return String.valueOf(num);
    }

    // --- MỚI: gửi OTP qua email (fallback)
    public boolean sendOtpByEmail(String toEmail, String otp) {
        if (toEmail == null || toEmail.isBlank()) return false;
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(toEmail);
            msg.setSubject("Mã OTP xác thực");
            msg.setText("Mã OTP của bạn là: " + otp + "\nMã này có hiệu lực trong vài phút.");
            msg.setFrom("no-reply@example.com");
            mailSender.send(msg);
            System.out.println("OTP email sent to: " + toEmail);
            return true;
        } catch (Exception e) {
            System.err.println("Error sending OTP email to " + toEmail + " : " + e.getMessage());
            return false;
        }
    }

    // --- MỚI: thử Twilio trước, nếu thất bại tạo devOtp và cố gắng gửi email
    // Trả về null nếu Twilio đã gửi thành công; trả về devOtp nếu fallback được tạo
    public String sendOtpWithFallback(String phoneNumber, String email) {
        boolean sent = sendOtp(phoneNumber);
        if (sent) return null;
        String devOtp = generateDevOtp();
        if (email != null && !email.isBlank()) {
            sendOtpByEmail(email, devOtp);
        }
        return devOtp;
    }
}