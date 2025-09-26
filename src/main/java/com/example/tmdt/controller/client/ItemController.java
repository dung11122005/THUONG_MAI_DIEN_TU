package com.example.tmdt.controller.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import net.minidev.json.JSONObject;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.tmdt.domain.Cart;
import com.example.tmdt.domain.CartDetail;
import com.example.tmdt.domain.Comment;
import com.example.tmdt.domain.Order;
import com.example.tmdt.domain.OrderDetail;
import com.example.tmdt.domain.Product;
import com.example.tmdt.domain.Product_;
import com.example.tmdt.domain.User;
import com.example.tmdt.domain.dto.ProductCriteriaDTO;
import com.example.tmdt.service.CommentService;
import com.example.tmdt.service.OrderService;
import com.example.tmdt.service.ProductService;
import com.example.tmdt.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class ItemController {

    private final ProductService productService;
    private final OrderService orderService;
    private final UserService userService;
    private final CommentService commentService;

    public ItemController(ProductService productService,
            OrderService orderService,
            UserService userService,
            CommentService commentService) {
        this.productService = productService;
        this.orderService = orderService;
        this.userService = userService;
        this.commentService = commentService;
    }

    @GetMapping("/product/{slug}")
    public String getProductPage(Model model, @PathVariable String slug) {
        // Lấy product theo slug
        Optional<Product> optionalPr = productService.fetchProductBySlug(slug);
    
        if (!optionalPr.isPresent()) {
            // Product không tồn tại, redirect về trang 404 hoặc danh sách sản phẩm
            return "redirect:/products";
        }
    
        Product pr = optionalPr.get();
    
        // Lấy product theo id (nếu cần, nhưng thường pr đã đầy đủ)
        Optional<Product> optionalProduct = productService.fetchProductById(pr.getId());
        Product prs = optionalProduct.orElse(pr); // fallback dùng pr nếu không có
        List<Comment> comments = prs.getComments(); // trả về null hoặc empty list
    
        // Lấy danh sách sản phẩm bán chạy và carousel
        List<Product> listProductBest = orderService.fetchBestSellingProductPage();
        List<Product> productCarousel = productService.fetchProducts();
    
        // Add vào model
        model.addAttribute("product", pr);
        model.addAttribute("products", listProductBest);
        model.addAttribute("productCarousel", productCarousel);
        model.addAttribute("comments", comments != null ? comments : Collections.emptyList());
        model.addAttribute("id", pr.getId());
    
        return "client/product/detail";
    }


    @PostMapping("/add-product-to-cart/{id}")
    public String addProductToCart(@PathVariable long id, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        long productId = id;
        String email = (String) session.getAttribute("email");
        this.productService.handleAddProductToCart(email, productId, session, 1);
        return "redirect:/";
    }

    @GetMapping("/cart")
    public String getCartPage(Model model, HttpServletRequest request) {
        User currenUser = new User();
        HttpSession session = request.getSession(false);
        long id = (long) session.getAttribute("id");
        currenUser.setId(id);

        Cart cart = this.productService.fetchByUser(currenUser);

        List<CartDetail> cds = cart == null ? new ArrayList<CartDetail>() : cart.getCartDetails();
        List<CartDetail> cartDetails = new ArrayList<CartDetail>();
        double totalPrice = 0;

        for (CartDetail cd : cds) {
            cd.setCheckbox(0);
            this.productService.saveCartDetail(cd);
            totalPrice += cd.getPrice() * cd.getQuantity();
            cartDetails.add(cd);
        }

        model.addAttribute("cartDetails", cartDetails);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("cart", cart);
        return "client/cart/show";
    }

    @PostMapping("/delete-cart-product/{id}")
    public String deleteCartDetail(@PathVariable long id, HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        long cartDetailId = id;
        this.productService.handleRemoveCartDetail(cartDetailId, session);
        return "redirect:/cart";
    }

    @GetMapping("/checkout")
    public String getCheckOutPage(Model model, HttpServletRequest request) {
        User currentUser = new User();// null
        HttpSession session = request.getSession(false);
        long id = (long) session.getAttribute("id");
        currentUser.setId(id);

        Cart cart = this.productService.fetchByUser(currentUser);

        List<CartDetail> cds = cart == null ? new ArrayList<CartDetail>() : cart.getCartDetails();
        List<CartDetail> cartDetails = new ArrayList<CartDetail>();
        for (CartDetail cd : cds) {
            if (cd.getCheckbox() != 0) {
                cartDetails.add(cd);
            }
        }

        double totalPrice = 0;
        for (CartDetail cd : cartDetails) {
            if (cd.getCheckbox() != 0) {
                totalPrice += cd.getPrice() * cd.getQuantity();
            }

        }

        model.addAttribute("cartDetails", cartDetails);
        model.addAttribute("totalPrice", totalPrice);

        return "client/cart/checkout";
    }

    @PostMapping("/confirm-checkout")
    public String getCheckOutPage(@ModelAttribute("cart") Cart cart) {
        List<CartDetail> cartDetails = cart == null ? new ArrayList<CartDetail>() : cart.getCartDetails();
        this.productService.handleUpdateCartBeforeCheckout(cartDetails);
        return "redirect:/checkout";
    }

    // @GetMapping("/thanks")
    // public String getThankYouPage(
    //         @RequestParam(value = "orderId", required = false) String orderId,
    //         @RequestParam(value = "resultCode", required = false) Integer resultCode,
    //         @RequestParam(value = "message", required = false) String message,
    //         HttpServletRequest request,
    //         Model model) throws Exception {

    //     // Kiểm tra trạng thái giao dịch từ resultCode
    //     // Kiểm tra trạng thái giao dịch từ resultCode
    //     if (resultCode != null && resultCode != 0) {
    //         // Nếu resultCode không phải 0, cho biết giao dịch thất bại
    //         return "client/cart/failure";
    //     } else {
    //         // Nếu không có resultCode trả về, kiểm tra trạng thái giao dịch qua
    //         // payment-status
    //         HttpSession session = request.getSession(false);
    //         String momoOrderId = (String) session.getAttribute("momoOrderId");
    //         String momoRequestId = (String) session.getAttribute("momoRequestId");

    //         if (momoOrderId != null && momoRequestId != null) {
    //             // Gọi phương thức checkPaymentStatus để kiểm tra trạng thái giao dịch
    //             String transactionStatus = paymentService.queryTransactionStatus(momoOrderId, momoRequestId);

    //             ObjectMapper objectMapper = new ObjectMapper();
    //             JsonNode jsonResponse = objectMapper.readTree(transactionStatus);

    //             int resultCodeFromApi = jsonResponse.path("resultCode").asInt();
    //             if (resultCodeFromApi == 0) {
    //                 return "client/cart/thanks";
    //             } else {
    //                 return "client/cart/failure";
    //             }
    //         }
    //     }
    //     return "client/cart/thanks";
    // }

    @GetMapping("/statusOrder/{id}")
    public String getStatusOrderYouPage(Model model, @PathVariable long id) {
        Optional<Order> opOrder = this.orderService.fetchOrderById(id);
        if (opOrder.isPresent()) {
            Order order = opOrder.get();
            List<OrderDetail> orderDetails = order.getOrderDetails();
            model.addAttribute("order", order);
            model.addAttribute("orderDetails", orderDetails);
        }
        return "client/cart/statusOrder";
    }

    // @PostMapping("/place-order")
    // public String handlePlaceOrder(
    //         HttpServletRequest request,
    //         @RequestParam("receiverName") String receiverName,
    //         @RequestParam("receiverAddress") String receiverAddress,
    //         @RequestParam("receiverPhone") String receiverPhone,
    //         @RequestParam("paymentMethod") String paymentMethod,
    //         RedirectAttributes redirectAttributes) throws Exception {
    //     User currentUser = new User();// null
    //     HttpSession session = request.getSession(false);
    //     long id = (long) session.getAttribute("id");
    //     currentUser.setId(id);

    //     // Giả sử bạn đã tạo một `orderId` và `orderInfo` từ quá trình đặt hàng
    //     String time = String.valueOf(System.currentTimeMillis());
    //     String orderId = "MOMO" + time;
    //     String orderInfo = "Payment for order " + orderId;
    //     String amount = Long.toString(this.productService.calculateTotalPrice(currentUser));
    //     String requestId = "MOMO" + time + "001";

    //     // Điều hướng thanh toán
    //     if ("MOMO".equalsIgnoreCase(paymentMethod)) {
    //         // Tạo yêu cầu thanh toán MOMO
    //         PaymentRequest paymentRequest = new PaymentRequest();
    //         paymentRequest.setAmount(amount);
    //         paymentRequest.setOrderId(orderId);
    //         paymentRequest.setOrderInfo(orderInfo);
    //         paymentRequest.setRequestId(requestId);
    //         paymentRequest.setExtraData("");

    //         // Gửi yêu cầu đến MOMO và nhận URL thanh toán
    //         String response = paymentService.createPayment(paymentRequest);

    //         // Phân tích JSON phản hồi
    //         ObjectMapper objectMapper = new ObjectMapper();
    //         JsonNode jsonResponse = objectMapper.readTree(response);

    //         // Lấy URL thanh toán từ JSON
    //         String paymentUrl = jsonResponse.path("payUrl").asText();

    //         // Kiểm tra nếu URL hợp lệ
    //         if (paymentUrl != null && !paymentUrl.isEmpty()) {
    //             // Trả về URL thanh toán và chuyển hướng người dùng đến đó
    //             // Lưu orderId và requestId vào session để kiểm tra sau
    //             session.setAttribute("momoOrderId", orderId);
    //             session.setAttribute("momoRequestId", requestId);

    //             return "redirect:" + paymentUrl;
    //         }
    //     } else if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
    //         return "redirect:/payment/vnpay";
    //     }
    //     // this.productService.handlePlaceOrder(currentUser, session,
    //     // receiverName,receiverAddress, receiverPhone);
    //     return "redirect:/thanks";
    // }

    @PostMapping("/add-product-from-view-detail")
    public String postMethodName(@RequestParam("id") long id,
            @RequestParam("quantity") long quantity,
            HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String email = (String) session.getAttribute("email");
        this.productService.handleAddProductToCart(email, id, session, quantity);
        return "redirect:/product/" + id;
    }

    @GetMapping("/products")
    public String getProductPage(Model model, ProductCriteriaDTO productCriteriaDTO, HttpServletRequest request) {
        int page = 1;
        try {
            if (productCriteriaDTO.getPage().isPresent()) {
                page = Integer.parseInt(productCriteriaDTO.getPage().get());
            } else {

            }
        } catch (Exception e) {

        }

        Pageable pageable = PageRequest.of(page - 1, 15);
        if (productCriteriaDTO.getSort() != null && productCriteriaDTO.getSort().isPresent()) {
            String sort = productCriteriaDTO.getSort().get();
            if (sort.equals("gia-tang-dan")) {
                pageable = PageRequest.of(page - 1, 3, Sort.by(Product_.PRICE).ascending());
            } else if (sort.equals("gia-giam-dan")) {
                pageable = PageRequest.of(page - 1, 3, Sort.by(Product_.PRICE).descending());
            }
        }

        Page<Product> pr = this.productService.fetchProductPaginationWithSpec(pageable, productCriteriaDTO);
        try {
            if (productCriteriaDTO.getSearchValue().isPresent() && productCriteriaDTO.getSearchValue().get() != null) {
                List<Product> products = this.productService.fetchProducts();
                List<Product> productsElasticSearch = this.productService.findElasticSearch(products,
                        productCriteriaDTO.getSearchValue().get());
                pr = this.productService.ProductPaginationElastictSearch(pageable, productsElasticSearch);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
        List<Product> listproduct = pr.getContent().size() > 0 ? pr.getContent() : new ArrayList<Product>();

        String qs = request.getQueryString();
        if (qs != null && !qs.isBlank()) {
            // remove page
            qs = qs.replace("page=" + page, "");
        }

        model.addAttribute("products", listproduct);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pr.getTotalPages());
        model.addAttribute("queryString", qs);

        // ✅ thêm mấy list này vào model
        model.addAttribute("factories", Arrays.asList("APPLE","ASUS","LENOVO","DELL","LG","ACER"));
        model.addAttribute("targets", Arrays.asList("GAMING","SINHVIEN-VANPHONG","THIET-KE-DO-HOA","MONG-NHE","DOANH-NHAN"));
        model.addAttribute("prices", Arrays.asList("duoi-10-trieu","10-15-trieu","15-20-trieu","tren-20-trieu"));
        model.addAttribute("sorts", Arrays.asList("gia-tang-dan","gia-giam-dan","gia-nothing"));

        return "client/product/show";
    }

    @PostMapping("/confirm-comment")
    public String postConfirmComment(HttpServletRequest request,
            @RequestParam("radio-sort") String star,
            @RequestParam("description") String description,
            @RequestParam("id") long idProduct) {
        HttpSession session = request.getSession(false);
        User currentUser = new User();// null
        long id = (long) session.getAttribute("id");
        currentUser = this.userService.getUserById(id);
        Product product = new Product();
        Optional<Product> optionalProduct = this.productService.fetchProductById(idProduct);
        if (optionalProduct.isPresent()) {
            product = (Product) optionalProduct.get();
        }
        int sta = Integer.parseInt(star);
        this.commentService.handleConfirmComment(sta, description, idProduct, currentUser, product);
        return "redirect:/product/" + optionalProduct.get().getSlug();
    }

    @GetMapping("/purchase")
    public String getPurchaseYouPage(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        long id = (long) session.getAttribute("id");
        User user = this.userService.getUserById(id);
        model.addAttribute("purchases", user.getOrders());
        model.addAttribute("PENDING", null);
        model.addAttribute("SHIPPING", null);
        model.addAttribute("COMPLETE", null);
        model.addAttribute("CANCEL", null);

        for (Order order : user.getOrders()) {
            if (order.getStatus() == "1PENDING") {
                model.addAttribute("PENDING", "1PENDING");
            } else if (order.getStatus() == "2SHIPPING") {
                model.addAttribute("SHIPPING", "2SHIPPING");
            } else if (order.getStatus() == "3COMPLETE") {
                model.addAttribute("COMPLETE", "3COMPLETE");
            } else if (order.getStatus() == "4CANCEL") {
                model.addAttribute("CANCEL", "4CANCEL");
            }
        }
        return "client/profile/purchase";
    }

}
