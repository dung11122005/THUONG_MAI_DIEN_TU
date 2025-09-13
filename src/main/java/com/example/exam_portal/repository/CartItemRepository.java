package com.example.exam_portal.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.exam_portal.domain.Cart;
import com.example.exam_portal.domain.CartItem;
import com.example.exam_portal.domain.Course;

public interface CartItemRepository extends JpaRepository<CartItem, Long>{
    Optional<CartItem> findByCartAndCourse(Cart cart, Course course);
}
