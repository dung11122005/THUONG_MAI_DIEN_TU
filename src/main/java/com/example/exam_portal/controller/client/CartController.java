package com.example.exam_portal.controller.client;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.exam_portal.domain.Cart;
import com.example.exam_portal.domain.Course;
import com.example.exam_portal.domain.PaymentRequest;
import com.example.exam_portal.domain.Purchase;
import com.example.exam_portal.domain.User;
import com.example.exam_portal.service.CartService;
import com.example.exam_portal.service.CourseService;
import com.example.exam_portal.service.PaymentService;
import com.example.exam_portal.service.PurchaseService;
import com.example.exam_portal.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CartController {
    private CartService cartService;
    private final UserService userService;
    private final CourseService courseService;
    private final PaymentService paymentService;
    private final PurchaseService purchaseService;

    public CartController(CartService cartService, UserService userService, 
    CourseService courseService, PaymentService paymentService, PurchaseService purchaseService){
        this.cartService=cartService;
        this.userService=userService;
        this.courseService=courseService;
        this.paymentService=paymentService;
        this.purchaseService=purchaseService;
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
    public String confirmCart(@RequestParam("selectedCourses") List<Long> courseIds,
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
    
        return "client/cart/confirm";
    }

    @PostMapping("/cart/purchase")
    public String purchaseCart(@RequestParam("courseIds") List<Long> courseIds,
                               @RequestParam("paymentMethod") String paymentMethod,
                               HttpServletRequest request) throws Exception {
        Long userId = (Long) request.getSession(false).getAttribute("id");

        // Lấy danh sách khóa học
        List<Course> courses = courseIds.stream()
                .map(courseService::getCourseById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (courses.isEmpty()) {
            return "redirect:/cart/confirm?error=NoCourses";
        }

        // Tổng tiền
        float totalPrice = courses.stream()
                .map(Course::getPrice)
                .filter(Objects::nonNull)
                .reduce(0f, Float::sum);

        String time = String.valueOf(System.currentTimeMillis());
        String orderId = "MOMO" + time;
        String requestId = orderId + "001";

        String orderInfo = "Thanh toan " + courses.size() + " khoa hoc";

        // ✅ Encode extraData theo format "cart:userId:courseId1,courseId2,..."
        String extraDataString = "cart:" + userId + ":" + courseIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        String extraData = Base64.getEncoder()
                .encodeToString(extraDataString.getBytes(StandardCharsets.UTF_8));

        // Tạo request
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setAmount(String.valueOf((int) totalPrice));
        paymentRequest.setOrderId(orderId);
        paymentRequest.setOrderInfo(orderInfo);
        paymentRequest.setRequestId(requestId);
        paymentRequest.setExtraData(extraData);

        String response = this.paymentService.createPayment(paymentRequest);
        JsonNode json = new ObjectMapper().readTree(response);
        String payUrl = json.path("payUrl").asText();

        return (payUrl != null && !payUrl.isEmpty()) ? "redirect:" + payUrl : "redirect:/thanks";
    }



    @GetMapping("/thanks")
    public String handleMomoReturn(
            Model model,
            @RequestParam(value = "orderId", required = false) String orderId,
            @RequestParam(value = "resultCode", required = false) Integer resultCode
    ) throws Exception {
    
        if (orderId == null) {
            return "client/thank/failure"; // Không có orderId → lỗi
        }
    
        String requestId = orderId + "001"; 
        String statusResponse = paymentService.queryTransactionStatus(orderId, requestId);
        JsonNode json = new ObjectMapper().readTree(statusResponse);
    
        String encodedExtraData = json.path("extraData").asText();
        String decoded = "";
        if (!encodedExtraData.isEmpty()) {
            decoded = new String(Base64.getDecoder().decode(encodedExtraData), StandardCharsets.UTF_8);
        }
    
        int resultCodeFromApi = json.path("resultCode").asInt();
        if ((resultCode == null || resultCode != 0) || resultCodeFromApi != 0) {
            // Lỗi thanh toán → gửi course nếu có để hiển thị
            if (decoded.startsWith("single:")) {
                Long courseId = Long.parseLong(decoded.substring(7).split(",")[1]);
                Course course = courseService.getCourseById(courseId);
                model.addAttribute("course", course);
            }
            return "client/thank/failure";
        }
    
        List<Course> purchasedCourses = new ArrayList<>();
    
        if (decoded.startsWith("single:")) {
            String[] parts = decoded.substring(7).split(","); 
            Long userId = Long.parseLong(parts[0]);
            Long courseId = Long.parseLong(parts[1]);
        
            User user = userService.getUserById(userId);
            Course course = courseService.getCourseById(courseId);
        
            if (course != null && user != null &&
                !purchaseService.checkPurchaseStudentIdAndCourseId(userId, courseId)) {
                Purchase purchase = new Purchase();
                purchase.setCourse(course);
                purchase.setStudent(user);
                this.purchaseService.handleSavePurchase(purchase);
            }
        
            purchasedCourses.add(course);
            this.cartService.removeCoursesFromCart(userId, List.of(courseId));
        
        } else if (decoded.startsWith("cart:")) {
            String[] parts = decoded.substring(5).split(":"); 
            Long userId = Long.parseLong(parts[0]);
            List<Long> courseIds = Arrays.stream(parts[1].split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        
            User user = userService.getUserById(userId);
        
            for (Long courseId : courseIds) {
                Course course = courseService.getCourseById(courseId);
                if (course != null && user != null &&
                    !purchaseService.checkPurchaseStudentIdAndCourseId(userId, courseId)) {
                    Purchase purchase = new Purchase();
                    purchase.setCourse(course);
                    purchase.setStudent(user);
                    this.purchaseService.handleSavePurchase(purchase);
                }
                purchasedCourses.add(course);
            }
        
            this.cartService.removeCoursesFromCart(userId, courseIds);
        }
    
        model.addAttribute("courses", purchasedCourses);
        return "client/thank/thank"; // thành công
    }

}
