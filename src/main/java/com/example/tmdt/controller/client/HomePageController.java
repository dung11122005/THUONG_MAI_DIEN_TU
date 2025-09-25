package com.example.tmdt.controller.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.tmdt.domain.Cart;
import com.example.tmdt.domain.CartDetail;
import com.example.tmdt.domain.Product;
import com.example.tmdt.domain.User;
import com.example.tmdt.repository.CartRepository;
import com.example.tmdt.service.OrderService;
import com.example.tmdt.service.ProductService;
import com.example.tmdt.service.UploadService;
import com.example.tmdt.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;


@Controller
public class HomePageController {
    

    private final ProductService productService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final OrderService orderService;
    private final UploadService uploadService;
    private final CartRepository cartRepository;

    public HomePageController(
            ProductService productService,
            UserService userService,
            PasswordEncoder passwordEncoder,
            OrderService orderService,
            UploadService uploadService,
            CartRepository cartRepository) {
        this.productService = productService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.orderService = orderService;
        this.uploadService = uploadService;
        this.cartRepository = cartRepository;
    }

    @GetMapping("/login")
    public String getLoginPage(Model model) {
        return "client/auth/login";
    }

    @GetMapping("/")
    public String getHomePage(Model model,
            @RequestParam("page") Optional<String> pageOptional, HttpServletRequest request) {
        try {
            User currentUser = new User();// null
            HttpSession session = request.getSession(false);
            long id = (long) session.getAttribute("id");
            currentUser.setId(id);
            User user = this.userService.getUserById(id);
            session.setAttribute("listOrder", this.userService.getOrdersSortedById(user));
            Cart cart = this.cartRepository.findByUser(currentUser);
            List<CartDetail> cartDetails = cart == null ? new ArrayList<CartDetail>() : cart.getCartDetails();
            int sum = 0;
            for (CartDetail cd : cartDetails) {
                if (cd != null) {
                    sum++;
                }
            }
            if (cart != null) {
                cart.setSum(sum);
                this.cartRepository.save(cart);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }

        int page = 1;
        try {
            if (pageOptional.isPresent()) {
                page = Integer.parseInt(pageOptional.get());
            } else {
                page = 1;
            }
        } catch (Exception e) {

        }
        Pageable pageable = PageRequest.of(page - 1, 8);
        Page<Product> prs = this.productService.fetchProductPagination(pageable);
        List<Product> products = prs.getContent();
        List<Product> bestProducts = this.orderService.fetchBestSellingProductPage();
        List<Product> productCarousel = this.productService.fetchProducts();
        model.addAttribute("bestProducts", bestProducts);
        model.addAttribute("products", products);
        model.addAttribute("productCarousel", productCarousel);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", prs.getTotalPages());

        return "client/homepage/show";
    }

}
