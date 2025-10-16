package com.example.tmdt.controller.client;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.tmdt.domain.Order;
import com.example.tmdt.domain.dto.PaymentRequest;
import com.example.tmdt.service.OrderService;
import com.example.tmdt.service.PaymentFactory;
import com.example.tmdt.service.PaymentService;

import jakarta.servlet.http.HttpServletRequest;


@Controller
public class PaymentController {
    private final PaymentFactory paymentFactory;
    private final OrderService orderService;



    public PaymentController(PaymentFactory paymentFactory, OrderService orderService) {
        this.paymentFactory = paymentFactory;
        this.orderService = orderService;
    }

    @PostMapping("/place-order")
    public String placeOrder(
        @RequestParam double totalPrice, @RequestParam String receiverName,
                             @RequestParam String receiverAddress,
                             @RequestParam String receiverPhone,
                             @RequestParam String paymentMethod,
                             HttpServletRequest servletRequest,
                             Model model) {

        Order order = this.orderService.createOrder(totalPrice, receiverName, receiverAddress, receiverPhone, paymentMethod);

        if ("COD".equalsIgnoreCase(paymentMethod)) {
            orderService.markAsWaitingDelivery(order.getId());
            model.addAttribute("success", true);
            model.addAttribute("orderId", order.getId());
            return "client/cart/thanks";
        }

        PaymentRequest request = new PaymentRequest(paymentMethod, order.getTotalPrice(), String.valueOf(order.getId()));
        PaymentService service = this.paymentFactory.getProvider(request.getProvider());
        String paymentUrl = service.createPaymentUrl(request, servletRequest);
        return "redirect:" + paymentUrl;
    }

    @GetMapping("/thank/{provider}")
    public String handleReturn(@PathVariable String provider,
                               @RequestParam Map<String, String> params,
                               Model model) {
        PaymentService service = this.paymentFactory.getProvider(provider);
        boolean success = service.verifyReturn(params);
                            
        // Sửa lại phần này để xử lý cả PayPal và VNPay
        String orderId;
        if ("paypal".equalsIgnoreCase(provider)) {
            orderId = params.get("orderId"); // Lấy từ parameter đã được thêm vào URL
        } else {
            orderId = params.getOrDefault("vnp_TxnRef", params.get("orderId"));
        }
    
        if (success) {
            orderService.markAsPaid(Long.parseLong(orderId));
        } else {
            orderService.markAsFailed(Long.parseLong(orderId));
        }
    
        model.addAttribute("success", success);
        model.addAttribute("orderId", orderId);
        return "client/cart/thanks";
    }
}
