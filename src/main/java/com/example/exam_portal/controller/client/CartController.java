package com.example.exam_portal.controller.client;

import com.example.exam_portal.service.CartService;

public class CartController {
    private CartService cartService;

    public CartController(CartService cartService){
        this.cartService=cartService;
    }
}
