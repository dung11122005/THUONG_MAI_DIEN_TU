package com.example.tmdt.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.example.tmdt.service.CustomUserDetailsService;
import com.example.tmdt.service.UploadService;
import com.example.tmdt.service.UserService;
import com.example.tmdt.service.userinfo.CustomOAuth2UserService;

import jakarta.servlet.DispatcherType;


@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

    @Autowired
    private UserService userService;

    @Autowired
    private UploadService uploadService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserService userService) {
        return new CustomUserDetailsService(userService);
    }

    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                     PasswordEncoder passwordEncoder) {
            DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
            authProvider.setUserDetailsService(userDetailsService);
            authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationSuccessHandler customSuccessHandler() {
        return new CustomSuccessHandler(); // bạn đã định nghĩa class này
    }

    @Bean
    public CustomOAuth2UserService customOAuth2UserService() {
        return new CustomOAuth2UserService(userService, uploadService);
    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            UserService userService,
                                UserDetailsService userDetailsService) throws Exception { // https://docs.spring.io/spring-security/reference/servlet/configuration/java.html#jc-httpsecurity
        http
                .authorizeHttpRequests(authorize -> authorize

                    .dispatcherTypeMatchers(DispatcherType.FORWARD,
                            DispatcherType.INCLUDE)
                    .permitAll()
                    .requestMatchers("/", "/register", "/products", "/product/**",
                            "/client/**", "/css/**", "/js/**", "/images/**", "/uploads/**", "/sitemap.xml", "/robots.txt") // https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html#match-requests
                    .permitAll()
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    .requestMatchers("/shipped/**").hasAnyRole("SHIPPED", "ADMIN")
                    .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2.loginPage("/login")
                    .successHandler(customSuccessHandler())
                    .failureUrl("/login?error")
                    .userInfoEndpoint(user -> user
                            .userService(customOAuth2UserService())))

                .formLogin(form -> form
                    .loginPage("/login")
                    .successHandler(customSuccessHandler())
                    .failureUrl("/login?error")
                    .permitAll()
                )
                                    
                .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login?logout")
                    .deleteCookies("JSESSIONID")
                    .invalidateHttpSession(true)
                )
                                    
                .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                    .invalidSessionUrl("/login?expired")
                    .maximumSessions(1)
                    .maxSessionsPreventsLogin(false)
                )
                    
                                    
                .rememberMe(remember -> remember
                    .key("uniqueAndSecretKey")
                    .rememberMeParameter("remember-me") // tùy tên checkbox
                    .tokenValiditySeconds(10000)
                    .userDetailsService(userDetailsService) // 👈 BẮT BUỘC
                )
                                    
                .exceptionHandling(ex -> ex.accessDeniedPage("/access-deny"));

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http,
                                                   AuthenticationProvider authenticationProvider) throws Exception {
    return http.getSharedObject(AuthenticationManagerBuilder.class)
               .authenticationProvider(authenticationProvider)
               .build();
    }
}
