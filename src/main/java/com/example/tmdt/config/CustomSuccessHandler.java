package com.example.tmdt.config;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.example.tmdt.domain.User;
import com.example.tmdt.service.OtpService;
import com.example.tmdt.service.UserService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class CustomSuccessHandler implements AuthenticationSuccessHandler {
    private final UserService userService;
    private final OtpService otpService;

    public CustomSuccessHandler(UserService userService, OtpService otpService) {
        this.userService = userService;
        this.otpService = otpService;
    }

    protected String determineTargetUrl(final Authentication authentication) {
        Map<String, String> roleTargetUrlMap = new HashMap<>();
        roleTargetUrlMap.put("ROLE_USER", "/");
        roleTargetUrlMap.put("ROLE_ADMIN", "/admin");
        roleTargetUrlMap.put("ROLE_SHIPPED", "/shipped");

        final Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        for (final GrantedAuthority grantedAuthority : authorities) {
            String authorityName = grantedAuthority.getAuthority();
            if (roleTargetUrlMap.containsKey(authorityName)) {
                return roleTargetUrlMap.get(authorityName);
            }
        }

        throw new IllegalStateException();
    }

    // chỉ xóa attribute exception (không populate session user ở đây nếu dùng OTP)
    protected void clearAuthenticationAttributes(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }

    // populate session user attributes (gọi khi login hoàn tất / sau verify OTP)
    protected void populateSessionAttributes(HttpServletRequest request, User user) {
        HttpSession session = request.getSession();
        if (user != null) {
            session.setAttribute("fullName", user.getFullName());
            session.setAttribute("avatar", user.getAvatar());
            session.setAttribute("address", user.getAddress());
            session.setAttribute("phone", user.getPhone());
            session.setAttribute("id", user.getId());
            session.setAttribute("email", user.getEmail());
            session.setAttribute("listOrder", this.userService.getOrdersSortedById(user));
            int sum = user.getCart() == null ? 0 : user.getCart().getSum();
            session.setAttribute("sum", sum);
        }
    }

    private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    private String generateDevOtp() {
        Random r = new Random();
        int num = 100000 + r.nextInt(900000);
        return String.valueOf(num);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        String targetUrl = determineTargetUrl(authentication);

        if (response.isCommitted()) {
            return;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        String email = authentication.getName();
        User user = this.userService.getUserByEmail(email);

        if (isAdmin) {
            populateSessionAttributes(request, user);
            clearAuthenticationAttributes(request);
            redirectStrategy.sendRedirect(request, response, targetUrl);
            return;
        }

        HttpSession session = request.getSession();
        session.setAttribute("pendingUser", user);
        session.setAttribute("targetUrlAfterOtp", targetUrl);

        // Gọi Twilio rồi fallback gửi email bằng OtpService
        String devOtp = null;
        if (user != null) {
            devOtp = otpService.sendOtpWithFallback(user.getPhone(), user.getEmail());
        }

        if (devOtp != null) {
            // không gửi được OTP qua Twilio -> thông báo và tạo mã dev để test local (devOtp đã tạo ở OtpService)
            String msg = "Không thể gửi OTP (kiểm tra Twilio hoặc số chưa được xác minh).";
            session.setAttribute("otpSendError", msg);
            session.setAttribute("devOtp", devOtp);
            System.out.println("DEV OTP (for testing only): " + devOtp);
        } else {
            session.removeAttribute("otpSendError");
            session.removeAttribute("devOtp");
        }

        clearAuthenticationAttributes(request);
        redirectStrategy.sendRedirect(request, response, "/verify-otp");
    }

}


