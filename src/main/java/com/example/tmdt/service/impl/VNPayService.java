package com.example.tmdt.service.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.tmdt.domain.dto.PaymentRequest;
import com.example.tmdt.service.PaymentService;

import jakarta.servlet.http.HttpServletRequest;


@Service
public class VNPayService implements PaymentService{
    @Value("${dung.vnpay.tmn-code}")
    private String vnp_TmnCode;

    @Value("${dung.vnpay.hash-secret}")
    private String secretKey;

    @Value("${dung.vnpay.vnp-return-url}")
    private String vnp_ReturnUrl;

    @Value("${dung.vnpay.vnp-url}")
    private String vnp_PayUrl;

    @Override
    public String getName() {
        return "vnpay";
    }

    @Override
    public String createPaymentUrl(PaymentRequest request, HttpServletRequest servletRequest) {
        try {
            return generateVNPayURL(request.getAmount(), request.getOrderId(), getIpAddress(servletRequest));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error generating VNPay URL", e);
        }
    }

    @Override
    public boolean verifyReturn(Map<String, String> params) {
        String vnp_SecureHash = params.get("vnp_SecureHash");

        // Bỏ SecureHash ra khỏi params để build lại data
        Map<String, String> sortedParams = new TreeMap<>(params);
        sortedParams.remove("vnp_SecureHash");
        sortedParams.remove("vnp_SecureHashType");

        StringBuilder hashData = new StringBuilder();
        for (Iterator<Map.Entry<String, String>> it = sortedParams.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, String> entry = it.next();
            hashData.append(entry.getKey()).append("=").append(entry.getValue());
            if (it.hasNext()) {
                hashData.append("&");
            }
        }

        String signed = hmacSHA512(secretKey, hashData.toString());

        return signed.equals(vnp_SecureHash) && "00".equals(params.get("vnp_ResponseCode"));
    }

    private String generateVNPayURL(double amountDouble, String paymentRef, String ip)
            throws UnsupportedEncodingException {

        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String orderType = "other";
        long amount = (long) amountDouble * 100;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", paymentRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang:" + paymentRef);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", ip);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();

        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII))
                     .append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String vnp_SecureHash = hmacSHA512(secretKey, hashData.toString());
        query.append("&vnp_SecureHash=").append(vnp_SecureHash);
        return vnp_PayUrl + "?" + query;
    }

    private String hmacSHA512(final String key, final String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    private String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        return (ipAddress != null) ? ipAddress : request.getRemoteAddr();
    }
}
