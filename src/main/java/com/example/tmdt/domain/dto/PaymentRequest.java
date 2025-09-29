package com.example.tmdt.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRequest {
    private String provider; // "vnpay", "paypal", "momo"
    private double amount;     // số tiền thanh toán
    private String orderId;  // mã đơn hàng (TxnRef)
    

    public PaymentRequest(String provider, double amount, String orderId) {
        this.provider = provider;
        this.amount = amount;
        this.orderId = orderId;
    }
}
