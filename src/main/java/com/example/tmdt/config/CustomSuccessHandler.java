package com.example.tmdt.config;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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

    protected void clearAuthenticationAttributes(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }

    private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    // Trong phương thức onAuthenticationSuccess

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
        // Admin không cần xác thực OTP
        userService.populateUserSession(request.getSession(), user);
        clearAuthenticationAttributes(request);
        redirectStrategy.sendRedirect(request, response, targetUrl);
        return;
    }

    HttpSession session = request.getSession();
    session.setAttribute("pendingUser", user);
    session.setAttribute("targetUrlAfterOtp", targetUrl);

    // Gửi OTP qua email hoặc hiển thị cho dev
    String devOtp = null;
    if (user != null) {
        devOtp = otpService.sendOtpWithFallback(user.getPhone(), user.getEmail());
    }

    // Lưu OTP vào session để hiển thị trong môi trường phát triển
    if (devOtp != null) {
        session.setAttribute("devOtp", devOtp);
    }

    clearAuthenticationAttributes(request);
    redirectStrategy.sendRedirect(request, response, "/verify-otp");
}
}