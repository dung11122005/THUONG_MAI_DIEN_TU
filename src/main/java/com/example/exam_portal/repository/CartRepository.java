package com.example.exam_portal.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.exam_portal.domain.Cart;
import com.example.exam_portal.domain.User;


@Repository
public interface  CartRepository extends JpaRepository<Cart, Long>{
    Optional<Cart> findByStudent(User student);
}
