package com.example.tmdt.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TwilioConfig {
    @Value("${twilio.account_sid}")
    private String accountSid;

    @Value("${twilio.auth_token}")
    private String authToken;

    @Value("${twilio.service_sid}")
    private String serviceSid;

    public String getAccountSid() { return accountSid; }
    public String getAuthToken() { return authToken; }
    public String getServiceSid() { return serviceSid; }
}

