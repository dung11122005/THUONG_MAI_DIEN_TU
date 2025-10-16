package com.example.tmdt.controller.client;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.tmdt.domain.User;
import com.example.tmdt.service.OtpService;
import com.example.tmdt.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class OtpController {

    private final OtpService otpService;
    private final UserService userService;

    @Autowired
    public OtpController(OtpService otpService, UserService userService) {
        this.otpService = otpService;
        this.userService = userService;
    }

    @GetMapping("/verify-otp")
    public String showOtpForm(Model model, HttpSession session) {
        if (session.getAttribute("pendingUser") == null) {
            return "redirect:/login";
        }
        return "client/auth/verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam("otp") String otp, 
                            HttpSession session, 
                            Model model,
                            HttpServletResponse response) throws IOException {
        
        User user = (User) session.getAttribute("pendingUser");
        String targetUrl = (String) session.getAttribute("targetUrlAfterOtp");
        
        if (user == null) {
            return "redirect:/login";
        }
        
        // Xác thực OTP bằng số điện thoại làm key
        boolean isValid = otpService.validateOtp(user.getPhone(), otp);
        
        if (isValid) {
            // Xóa OTP đã sử dụng
            otpService.clearOtp(user.getPhone());
            
            // Xóa session tạm thời
            session.removeAttribute("pendingUser");
            session.removeAttribute("targetUrlAfterOtp");
            session.removeAttribute("otpSendError");
            session.removeAttribute("devOtp");
            
            // Tạo session người dùng chính thức
            userService.populateUserSession(session, user);
            
            // Chuyển hướng về trang đích
            if (targetUrl != null) {
                return "redirect:" + targetUrl;
            } else {
                return "redirect:/";
            }
        } else {
            model.addAttribute("error", "Mã OTP không đúng hoặc đã hết hạn!");
            return "client/auth/verify-otp";
        }
    }
    
    // Thêm phương thức resendOtp
    @GetMapping("/resend-otp")
    public String resendOtp(HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("pendingUser");
        if (user == null) {
            return "redirect:/login";
        }

        // Gửi lại OTP
        String devOtp = otpService.sendOtpWithFallback(user.getPhone(), user.getEmail());

        // Lưu OTP vào session để hiển thị
        if (devOtp != null) {
            session.setAttribute("devOtp", devOtp);
        }

        // Thông báo đã gửi lại OTP
        redirectAttributes.addFlashAttribute("otpResent", true);

        return "redirect:/verify-otp";
    }
}