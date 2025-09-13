package com.example.exam_portal.controller.client;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.exam_portal.domain.Cart;
import com.example.exam_portal.domain.Course;
import com.example.exam_portal.domain.User;
import com.example.exam_portal.service.CartService;
import com.example.exam_portal.service.CourseService;
import com.example.exam_portal.service.UserService;

@Controller
public class CartController {
    private CartService cartService;
    private final UserService userService;
    private final CourseService courseService;

    public CartController(CartService cartService, UserService userService, CourseService courseService){
        this.cartService=cartService;
        this.userService=userService;
        this.courseService=courseService;
    }


    // Thêm khóa học
    @GetMapping("/cart/add/{id}")
    public String addCourse(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        // Lấy entity User từ DB
        String username = userDetails.getUsername();
        User currentUser = this.userService.getUserByEmail(username);
        if (currentUser == null) {
            throw new RuntimeException("User not found");
        }

        this.cartService.addCourseToCart(currentUser.getId(), id);
        return "redirect:/cart/view";
    }

    // Xóa khóa học
    @GetMapping("/cart/remove/{id}")
    public String removeCourse(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        // Lấy entity User từ DB
        String username = userDetails.getUsername();
        User currentUser = this.userService.getUserByEmail(username);
        if (currentUser == null) {
            throw new RuntimeException("User not found");
        }

        this.cartService.removeCourseFromCart(currentUser.getId(), id);
        return "redirect:/cart/view";
    }


    // Xem giỏ hàng
    @GetMapping("/cart/view")
    public String viewCart(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        // Lấy username từ UserDetails
        String username = userDetails.getUsername();

        // Lấy entity User từ DB
        User currentUser = this.userService.getUserByEmail(username);
        if (currentUser == null) {
            throw new RuntimeException("User not found");
        }


        Cart cart = cartService.getCartByStudent(currentUser);
        model.addAttribute("cart", cart);
        return "client/cart/view";
    }

    // Xử lý thanh toán nhiều khóa học
    @GetMapping("/cart/confirm")
        public String confirmCart(@RequestParam("courseIds") List<Long> courseIds,
                                  Model model) {
            if (courseIds == null || courseIds.isEmpty()) {
                model.addAttribute("error", "Bạn chưa chọn khóa học nào!");
                return "client/cart/confirm";
            }
        
            // Lấy danh sách khóa học từ DB
            List<Course> selectedCourses = courseIds.stream()
                    .map(courseService::getCourseById)
                    .filter(Objects::nonNull) // tránh null nếu id sai
                    .collect(Collectors.toList());
        
            // Tính tổng tiền (Float)
            float totalPrice = selectedCourses.stream()
                .map(Course::getPrice)
                .filter(Objects::nonNull)
                .reduce(0f, Float::sum);

        
            model.addAttribute("selectedCourses", selectedCourses);
            model.addAttribute("totalPrice", totalPrice);
        
            return "client/cart/confirm"; // -> thymeleaf view
        }



}
