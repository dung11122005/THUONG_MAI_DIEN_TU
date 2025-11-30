package com.example.tmdt.service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Service
public class OtpService {
    
    private final JavaMailSender mailSender;
    private final VonageService vonageService;
    private LoadingCache<String, String> otpCache;
    
    @Value("${app.environment:dev}")
    private String environment;
    
    @Autowired
    public OtpService(JavaMailSender mailSender, VonageService vonageService) {
        this.mailSender = mailSender;
        this.vonageService = vonageService;
        
        // Khởi tạo cache lưu OTP với thời gian sống 3 phút
        otpCache = CacheBuilder.newBuilder()
                .expireAfterWrite(3, TimeUnit.MINUTES)
                .build(new CacheLoader<String, String>() {
                    @Override
                    public String load(String key) throws Exception {
                        return "";
                    }
                });
    }
    
    /**
     * Gửi OTP qua SMS hoặc email
     * @param phone Số điện thoại nhận OTP
     * @param email Email dự phòng để gửi OTP
     * @return Mã OTP được tạo (chỉ trong môi trường dev)
     */
    public String sendOtpWithFallback(String phone, String email) {
        // Tạo mã OTP
        String otp = vonageService.generateOtp();
        
        // Lưu OTP vào cache (sử dụng phone làm key)
        saveOtpForValidation(phone, otp);
        
        boolean sent = false;
        
        // Ưu tiên gửi qua SMS sử dụng Vonage
        // if (phone != null && !phone.isEmpty()) {
        //     try {
        //         sent = vonageService.sendOtp(phone, otp);
        //         if (sent) {
        //             System.out.println("OTP sent successfully via Vonage to: " + phone);
        //         } else {
        //             System.err.println("Failed to send OTP via Vonage");
        //         }
        //     } catch (Exception e) {
        //         System.err.println("Error sending OTP via Vonage: " + e.getMessage());
        //         sent = false;
        //     }
        // }
        
        // Nếu gửi SMS thất bại hoặc đang trong môi trường dev, gửi qua email
        // if ((!sent || "dev".equals(environment)) && email != null && !email.isEmpty()) {
        //     try {
        //         sendOtpEmail(email, otp);
        //         System.out.println("OTP sent via email to: " + email);
        //         sent = true;
        //     } catch (Exception ex) {
        //         System.err.println("Failed to send OTP via email: " + ex.getMessage());
        //     }
        // }
        
        // In ra console để kiểm tra trong môi trường phát triển
        System.out.println("===== DEV OTP INFORMATION =====");
        System.out.println("Phone: " + phone);
        System.out.println("OTP: " + otp);
        System.out.println("==============================");
        
        // Trả về OTP cho môi trường phát triển
        return otp;
    }
    
    // Gửi OTP qua email
    // private void sendOtpEmail(String email, String otp) {
    //     SimpleMailMessage message = new SimpleMailMessage();
    //     message.setTo(email);
    //     message.setSubject("Mã xác thực OTP từ Laptopshop");
    //     message.setText("Mã xác thực OTP của bạn là: " + otp + 
    //                     "\nMã có hiệu lực trong 3 phút.");
    //     mailSender.send(message);
    // }
    
    // Các phương thức còn lại giữ nguyên
    public String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // 6 chữ số
        return String.valueOf(otp);
    }
    
    public void saveOtpForValidation(String key, String otp) {
        otpCache.put(key, otp);
    }
    
    public boolean validateOtp(String key, String otp) {
        try {
            String cachedOtp = otpCache.get(key);
            return cachedOtp != null && cachedOtp.equals(otp);
        } catch (Exception e) {
            return false;
        }
    }
    
    public void clearOtp(String key) {
        otpCache.invalidate(key);
    }
}