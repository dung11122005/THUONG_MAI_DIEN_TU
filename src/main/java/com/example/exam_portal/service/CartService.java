package com.example.exam_portal.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.exam_portal.domain.Cart;
import com.example.exam_portal.domain.CartItem;
import com.example.exam_portal.domain.Course;
import com.example.exam_portal.domain.User;
import com.example.exam_portal.repository.CartItemRepository;
import com.example.exam_portal.repository.CartRepository;
import com.example.exam_portal.repository.CourseRepository;
import com.example.exam_portal.repository.UserRepository;

import jakarta.transaction.Transactional;


@Service
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
    UserRepository userRepository, CourseRepository courseRepository){
        this.cartRepository=cartRepository;
        this.userRepository=userRepository;
        this.cartItemRepository=cartItemRepository;
        this.courseRepository=courseRepository;
    }


    @Transactional
    public void addCourseToCart(Long studentId, Long courseId) {
        User student = this.userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        Course course = this.courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Cart cart = this.cartRepository.findByStudent(student)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setStudent(student);
                    return this.cartRepository.save(newCart);
                });

        boolean exists = cart.getItems().stream()
                .anyMatch(item -> item.getCourse().getId().equals(courseId));

        if (!exists) {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setCourse(course);
            cart.getItems().add(item);
            this.cartRepository.save(cart);
        }
    }


    @Transactional
    public void removeCourseFromCart(Long studentId, Long courseId) {
        User student = this.userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Cart cart = this.cartRepository.findByStudent(student)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getCourse().getId().equals(courseId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Course not in cart"));

        cart.getItems().remove(item);
        this.cartItemRepository.delete(item); // orphanRemoval = true cũng sẽ xóa nhưng gọi explicit cũng ok
    }


    public Cart getCartByStudent(User student) {
        return this.cartRepository.findByStudent(student).orElse(new Cart());
    }


    public List<Course> getSelectedCourses(List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Collections.emptyList();
        }
        return this.courseRepository.findByIdIn(courseIds);
    }

}
