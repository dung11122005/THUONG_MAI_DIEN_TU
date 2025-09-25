package com.example.tmdt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.tmdt.domain.OrderDetail;


@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {

}
