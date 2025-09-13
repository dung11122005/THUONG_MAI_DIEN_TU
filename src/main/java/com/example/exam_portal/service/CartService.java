package com.example.exam_portal.service;

import com.example.exam_portal.repository.CartRepository;

public class CartService {
    private final CartRepository cartRepository;

    public CartService(CartRepository cartRepository){
        this.cartRepository=cartRepository;
    }
}
