package com.example.tmdt.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.tmdt.domain.Order;
import com.example.tmdt.domain.OrderDetail;
import com.example.tmdt.domain.Product;
import com.example.tmdt.domain.User;
import com.example.tmdt.domain.dto.ProductCriteriaDTO;
import com.example.tmdt.repository.OrderDetailRepository;
import com.example.tmdt.repository.OrderRepository;
import com.example.tmdt.repository.ProductRepository;


import com.example.tmdt.service.Specification.ProductSpecs;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;
    private final UploadService uploadService;

    public OrderService(OrderRepository orderRepository,
            OrderDetailRepository orderDetailRepository,
            ProductRepository productRepository, UploadService uploadService) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.productRepository = productRepository;
        this.uploadService = uploadService;
    }

    public List<Order> fetchAllOrders() {
        return this.orderRepository.findAll();
    }

    public Page<Order> fetchAllOrdersPagination(Pageable page) {
        return this.orderRepository.findAll(page);
    }

    public Page<Order> fetchShipProductPaginationWithSpec(Pageable page, ProductCriteriaDTO productCriteriaDTO) {

        if (productCriteriaDTO.getShipSearchValue() == null) {
            return this.orderRepository.findAll(page);
        }

        Specification<Order> combinedSpec = Specification.where(null);
        if (productCriteriaDTO.getShipSearchValue() != null && productCriteriaDTO.getShipSearchValue().isPresent()) {
            Specification<Order> currentSpecs = ProductSpecs
                    .matchListShipSearch(productCriteriaDTO.getShipSearchValue().get());
            combinedSpec = combinedSpec.and(currentSpecs);
        }

        return this.orderRepository.findAll(combinedSpec, page);
    }

    public Optional<Order> fetchOrderById(long id) {
        return this.orderRepository.findById(id);
    }

    public void deleteOrderById(long id) {
        // delete order detail
        Optional<Order> orderOptional = this.fetchOrderById(id);
        if (orderOptional.isPresent()) {
            Order order = orderOptional.get();
            List<OrderDetail> orderDetails = order.getOrderDetails();
            for (OrderDetail orderDetail : orderDetails) {
                this.orderDetailRepository.deleteById(orderDetail.getId());
            }
        }

        this.orderRepository.deleteById(id);
    }

    public void updateOrder(Order order) {
        Optional<Order> orderOptional = this.fetchOrderById(order.getId());
        if (orderOptional.isPresent()) {

            Order currentOrder = orderOptional.get();
            currentOrder.setStatus(order.getStatus());
            this.orderRepository.save(currentOrder);
        }
    }

    public void updateOrder(Order order, MultipartFile file) {
        Optional<Order> orderOptional = this.fetchOrderById(order.getId());
        if (orderOptional.isPresent()) {
            Order currentOrder = orderOptional.get();

            if (!file.isEmpty()) {
                String img = this.uploadService.handleSaveUploadFile(file, "shipped");
                currentOrder.setImage(img);
            }
            currentOrder.setStatus(order.getStatus());
            this.orderRepository.save(currentOrder);
        }
    }

    public long countOrders() {
        return this.orderRepository.count();
    }

    public List<Order> fetchOrderByUser(User user) {
        return this.orderRepository.findByUser(user);
    }

    public List<Product> fetchBestSellingProductPage() {
        List<OrderDetail> orderDetails = this.orderDetailRepository.findAll();
        List<Long> numbers = new ArrayList<>();
        for (OrderDetail od : orderDetails) {
            for (int i = 0; i < od.getQuantity(); i++) {
                numbers.add(od.getProduct().getId());
            }

        }
        // Tạo một Map để lưu trữ số và số lần xuất hiện
        Map<Long, Integer> countMap = new HashMap<>();

        for (Long number : numbers) {
            countMap.put(number, countMap.getOrDefault(number, 0) + 1);
        }

        // Sắp xếp Map theo giá trị giảm dần
        List<Map.Entry<Long, Integer>> list = new ArrayList<>(countMap.entrySet());
        list.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        List<Product> products = new ArrayList<>();
        // In kết quả
        for (Map.Entry<Long, Integer> entry : list) {
            products.add(this.productRepository.findById(entry.getKey()).get());

        }
        return products;
    }

    public Order createOrder(double totalPrice,String  receiverName, String receiverAddress, String receiverPhone, String paymentMethod) {
        Order order = new Order();
        order.setReceiverName(receiverName);
        order.setReceiverAddress(receiverAddress);
        order.setReceiverPhone(receiverPhone);
        order.setStatus("PENDING");
        order.setTotalPrice(totalPrice);
        order.setPaymentMethod(paymentMethod);
        order.setCreatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    public void markAsPaid(Long orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus("PAID");
            orderRepository.save(order);
        });
    }

    public void markAsFailed(Long orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus("FAILED");
            orderRepository.save(order);
        });
    }

    public void markAsWaitingDelivery(Long orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus("WAITING_DELIVERY");
            orderRepository.save(order);
        });
    }

}
