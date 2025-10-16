package com.example.tmdt.controller.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.tmdt.service.VonageService;

@RestController
@RequestMapping("/api/sms-test")
public class SmsTestController {

    @Autowired
    private VonageService vonageService;
    
    @GetMapping("/send-otp")
    public ResponseEntity<String> sendTestSms(@RequestParam(required = false) String phone) {
        if (phone == null || phone.isEmpty()) {
            phone = "0374071674"; // Số điện thoại mặc định nếu không cung cấp
        }
        
        String otp = vonageService.generateOtp();
        boolean sent = vonageService.sendOtp(phone, otp);
        
        if (sent) {
            return ResponseEntity.ok("OTP sent successfully to " + phone + ". OTP: " + otp);
        } else {
            return ResponseEntity.status(500).body("Failed to send OTP to " + phone);
        }
    }
    
    @GetMapping("/balance")
    public ResponseEntity<String> checkBalance() {
        String info = vonageService.checkBalance();
        return ResponseEntity.ok(info);
    }
}