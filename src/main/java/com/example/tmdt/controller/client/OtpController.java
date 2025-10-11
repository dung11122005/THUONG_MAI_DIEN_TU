package com.example.tmdt.controller.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.tmdt.domain.User;
import com.example.tmdt.service.OtpService;
import com.example.tmdt.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class OtpController {

    @Autowired
    private OtpService otpService;

    @Autowired
    private UserService userService;

    @GetMapping("/verify-otp")
    public String showVerifyOtpPage(Model model, HttpSession session) {
        Object pendingUser = session.getAttribute("pendingUser");
        if (pendingUser == null) {
            return "redirect:/login";
        }
        // chuyển otpSendError và devOtp tới view nếu có
        return "client/auth/verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam("otp") String otp, HttpSession session, Model model) {
        Object userObj = session.getAttribute("pendingUser");

        if (userObj == null) {
            return "redirect:/login";
        }

        User user = (User) userObj;
        String phone = user.getPhone();

        boolean ok = false;
        // kiểm tra Twilio
        if (otpService.verifyOtp(phone, otp)) {
            ok = true;
        } else {
            // kiểm tra devOtp (nếu Twilio không gửi vì tài khoản trial)
            Object devOtp = session.getAttribute("devOtp");
            if (devOtp != null && otp != null && otp.equals(devOtp.toString())) {
                ok = true;
            }
        }

        if (ok) {
            User fresh = this.userService.getUserByEmail(user.getEmail());
            if (fresh != null) {
                session.setAttribute("fullName", fresh.getFullName());
                session.setAttribute("avatar", fresh.getAvatar());
                session.setAttribute("address", fresh.getAddress());
                session.setAttribute("phone", fresh.getPhone());
                session.setAttribute("id", fresh.getId());
                session.setAttribute("email", fresh.getEmail());
                session.setAttribute("listOrder", this.userService.getOrdersSortedById(fresh));
                int sum = fresh.getCart() == null ? 0 : fresh.getCart().getSum();
                session.setAttribute("sum", sum);
            }

            String targetUrl = (String) session.getAttribute("targetUrlAfterOtp");

            // Dọn session tạm
            session.removeAttribute("pendingUser");
            session.removeAttribute("targetUrlAfterOtp");
            session.removeAttribute("otpSendError");
            session.removeAttribute("devOtp");

            return "redirect:" + (targetUrl != null ? targetUrl : "/");
        } else {
            model.addAttribute("error", "Mã OTP không đúng hoặc đã hết hạn!");
            return "client/auth/verify-otp";
        }
    }
}