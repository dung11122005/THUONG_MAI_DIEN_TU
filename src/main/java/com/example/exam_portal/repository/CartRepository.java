package com.example.exam_portal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.exam_portal.domain.Cart;


@Repository
public interface  CartRepository extends JpaRepository<Cart, Long>{
    
}
