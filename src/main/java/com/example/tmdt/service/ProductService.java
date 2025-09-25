package com.example.tmdt.service;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.example.tmdt.domain.Cart;
import com.example.tmdt.domain.CartDetail;
import com.example.tmdt.domain.Order;
import com.example.tmdt.domain.OrderDetail;
import com.example.tmdt.domain.Product;
import com.example.tmdt.domain.User;
import com.example.tmdt.domain.dto.ProductCriteriaDTO;
import com.example.tmdt.repository.CartDetailRepository;
import com.example.tmdt.repository.CartRepository;
import com.example.tmdt.repository.OrderDetailRepository;
import com.example.tmdt.repository.OrderRepository;
import com.example.tmdt.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import jakarta.servlet.http.HttpSession;

import com.example.tmdt.service.Specification.ProductSpecs;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final CartDetailRepository cartDetailRepository;
    private final UserService userService;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ElasticsearchClient elasticsearchClient;

    public ProductService(
            ProductRepository productRepository,
            CartRepository cartRepository,
            CartDetailRepository cartDetailRepository,
            UserService userService,
            OrderRepository orderRepository,
            OrderDetailRepository orderDetailRepository,
            ElasticsearchClient elasticsearchClient) {
        this.productRepository = productRepository;
        this.cartDetailRepository = cartDetailRepository;
        this.cartRepository = cartRepository;
        this.userService = userService;
        this.orderDetailRepository = orderDetailRepository;
        this.orderRepository = orderRepository;
        this.elasticsearchClient = elasticsearchClient;
    }

    public Product createProduct(Product pr) {
        return this.productRepository.save(pr);
    }

    public CartDetail saveCartDetail(CartDetail pr) {
        return this.cartDetailRepository.save(pr);
    }

    public List<Product> fetchProducts() {
        return this.productRepository.findAll();
    }

    public Page<Product> fetchProductPagination(Pageable page) {
        return this.productRepository.findAll(page);
    }

    public Page<Product> ProductPaginationElastictSearch(Pageable pageable, List<Product> products) {

        // Tạo đối tượng Pageable
        // Pageable pageable = PageRequest.of(page.getPageNumber(), page.getPageSize());

        // Xác định chỉ số bắt đầu và kết thúc của sublist
        int start = Math.min((int) pageable.getOffset(), products.size());
        int end = Math.min((start + pageable.getPageSize()), products.size());

        // Tạo sublist từ danh sách products
        List<Product> subList = products.subList(start, end);

        // Tạo PageImpl với sublist và pageable
        Page<Product> productElasticSearch = new PageImpl<>(subList, pageable, products.size());
        return productElasticSearch;
    }

    public List<Product> findElasticSearch(List<Product> products, String name) {
        List<Product> productsEltS = new ArrayList<Product>();

        // createIndexIfNotExists("products");
        try {
            // for (Product product : products) {
            // ObjectMapper objectMapper = new ObjectMapper();
            // String jsonString = objectMapper.writeValueAsString(product);
            // StringReader stringReader = new StringReader(jsonString);
            // IndexRequest<Product> request = IndexRequest.of(i -> i
            // .index("products") // Tên chỉ mục
            // .id(String.valueOf(product.getId())) // ID của tài liệu
            // .withJson(stringReader) // Dữ liệu tài liệu
            // );

            // IndexResponse response = elasticsearchClient.index(request);
            // }

            SearchResponse<Product> searchResponse = elasticsearchClient.search(s -> s
                    .index("products")
                    .query(q -> q
                            .fuzzy(f -> f
                                    .field("name")
                                    .value(name)
                                    .fuzziness("AUTO"))),
                    Product.class);

            for (Hit<Product> hit : searchResponse.hits().hits()) {
                // Lấy tài liệu từ hit và thêm vào danh sách sản phẩm
                Product product = hit.source();
                productsEltS.add(product);
            }
            // System.out.println(">>>>>>>>>> productsEltS " + productsEltS);
        } catch (Exception e) {
            System.err.println("Error >>>>>>>>>>>> index >>: " + e.getMessage()); // In thông báo lỗi ra console
        }
        return productsEltS;
    }

    public Page<Product> fetchProductPaginationWithSpec(Pageable page, ProductCriteriaDTO productCriteriaDTO) {

        if (productCriteriaDTO.getTarget() == null &&
                productCriteriaDTO.getFactory() == null &&
                productCriteriaDTO.getPrice() == null &&
                productCriteriaDTO.getValueStar() == null) {
            return this.productRepository.findAll(page);
        }

        Specification<Product> combinedSpec = Specification.where(null);
        if (productCriteriaDTO.getTarget() != null && productCriteriaDTO.getTarget().isPresent()) {
            Specification<Product> currentSpecs = ProductSpecs.matchListTarget(productCriteriaDTO.getTarget().get());
            combinedSpec = combinedSpec.and(currentSpecs);
        }
        if (productCriteriaDTO.getFactory() != null && productCriteriaDTO.getFactory().isPresent()) {
            Specification<Product> currentSpecs = ProductSpecs.matchListFactory(productCriteriaDTO.getFactory().get());
            combinedSpec = combinedSpec.and(currentSpecs);
        }
        if (productCriteriaDTO.getPrice() != null && productCriteriaDTO.getPrice().isPresent()) {
            Specification<Product> currentSpecs = this.buildPriceSpecification(productCriteriaDTO.getPrice().get());
            combinedSpec = combinedSpec.and(currentSpecs);
        }
        if (productCriteriaDTO.getValueStar() != null && productCriteriaDTO.getValueStar().isPresent()) {
            Specification<Product> currentSpecs = ProductSpecs.matchListStar(productCriteriaDTO.getValueStar().get());
            combinedSpec = combinedSpec.and(currentSpecs);
        }
        // if (productCriteriaDTO.getSearchValue() != null &&
        // productCriteriaDTO.getSearchValue().isPresent()) {

        // Specification<Product> currentSpecs = ProductSpecs
        // .matchListSearch(productCriteriaDTO.getSearchValue().get());
        // combinedSpec = combinedSpec.and(currentSpecs);
        // }
        return this.productRepository.findAll(combinedSpec, page);
    }

    // case 6
    public Specification<Product> buildPriceSpecification(List<String> price) {
        Specification<Product> combinedSpec = Specification.where(null);

        for (String p : price) {
            double min = 0;
            double max = 0;

            // Set the appropriate min and max based on the price range string
            switch (p) {
                case "duoi-10-trieu":
                    min = 1;
                    max = 10000000;
                    break;
                case "10-15-trieu":
                    min = 10000000;
                    max = 15000000;
                    break;
                case "15-20-trieu":
                    min = 15000000;
                    max = 20000000;
                    break;
                case "tren-20-trieu":
                    min = 20000000;
                    max = 200000000;
                    break;
            }

            if (min != 0 && max != 0) {
                Specification<Product> rangeSpec = ProductSpecs.matchMultiplePrice(min, max);
                combinedSpec = combinedSpec.or(rangeSpec);
            }
        }

        return combinedSpec;
    }

    public Optional<Product> fetchProductById(long id) {
        return this.productRepository.findById(id);
    }

    public Optional<Product> fetchProductBySlug(String String) {
        return this.productRepository.findBySlug(String);
    }

    public void deleteProduct(long id) {
        this.productRepository.deleteById(id);
    }

    public void handleAddProductToCart(String email, long productId, HttpSession session, long quantity) {
        // check user đã có cart hay chưa nếu chưa -> tạo mới

        User user = this.userService.getUserByEmail(email);
        if (user != null) {
            Cart cart = this.cartRepository.findByUser(user);

            if (cart == null) {
                // tạo mới cart
                Cart otherCart = new Cart();

                otherCart.setUser(user);
                otherCart.setSum(0);

                cart = this.cartRepository.save(otherCart);
            }

            // save cart_detail
            // tìm product by id
            Optional<Product> optionalProduct = this.productRepository.findById(productId);
            if (optionalProduct.isPresent()) {
                Product realProduct = optionalProduct.get();

                // check sản phẩm đã từng được thêm vào giỏ hàng trước đây chưa ?
                CartDetail oldDetail = this.cartDetailRepository.findByCartAndProduct(cart,
                        realProduct);

                if (oldDetail == null) {
                    CartDetail cd = new CartDetail();
                    cd.setCart(cart);
                    cd.setProduct(realProduct);
                    cd.setPrice(realProduct.getPrice());
                    cd.setQuantity(quantity);
                    this.cartDetailRepository.save(cd);

                    // update cart (sum)
                    int s = cart.getSum() + 1;
                    cart.setSum(s);
                    this.cartRepository.save(cart);
                    session.setAttribute("sum", s);
                } else {
                    oldDetail.setQuantity(oldDetail.getQuantity() + quantity);
                    this.cartDetailRepository.save(oldDetail);
                }

            }

        }

    }

    public Cart fetchByUser(User user) {
        return this.cartRepository.findByUser(user);
    }

    public void handleRemoveCartDetail(long cartDetailId, HttpSession session) {
        try {
            Optional<CartDetail> cartDetailOptional = this.cartDetailRepository.findById(cartDetailId);

            if (cartDetailOptional.isPresent()) {
                CartDetail cartDetail = cartDetailOptional.get();

                Cart currentCart = cartDetail.getCart();
                // delete cart-detail
                this.cartDetailRepository.deleteById(cartDetailId);

                // update cart
                if (currentCart.getSum() > 1) {
                    // update current cart
                    int s = currentCart.getSum() - 1;
                    currentCart.setSum(s);
                    session.setAttribute("sum", s);
                    this.cartRepository.save(currentCart);
                } else {
                    // delete cart (sum=1)
                    this.cartRepository.deleteById(currentCart.getId());
                    session.setAttribute("sum", 0);
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
        }

    }

    public void handleUpdateCartBeforeCheckout(List<CartDetail> cartDetails) {
        for (CartDetail cartDetail : cartDetails) {
            Optional<CartDetail> cdOptional = this.cartDetailRepository.findById(cartDetail.getId());
            if (cdOptional.isPresent()) {
                CartDetail currentCartDetail = cdOptional.get();
                currentCartDetail.setQuantity(cartDetail.getQuantity());
                currentCartDetail.setCheckbox(cartDetail.getCheckbox());
                this.cartDetailRepository.save(currentCartDetail);
            }
        }
    }

    public long calculateTotalPrice(User user) {
        // Lấy giỏ hàng của người dùng
        Cart cart = this.cartRepository.findByUser(user);

        // Tổng giá trị thanh toán
        long totalPrice = 0;

        if (cart != null) {
            List<CartDetail> cartDetails = cart.getCartDetails();

            if (cartDetails != null) {
                for (CartDetail cd : cartDetails) {
                    if (cd.getCheckbox() != 0) { // Chỉ tính sản phẩm được chọn
                        totalPrice += cd.getPrice() * cd.getQuantity();
                    }
                }
            }
        }

        return totalPrice;
    }

    public void handlePlaceOrder(
            User user, HttpSession session,
            String receiverName, String receiverAddress, String receiverPhone) {

        // create order detail
        // step 1: get cart by user
        Cart cart = this.cartRepository.findByUser(user);

        if (cart != null) {
            List<CartDetail> cartDetails = cart.getCartDetails();

            if (cartDetails != null) {

                // create order
                Order order = new Order();
                order.setUser(user);
                order.setReceiverName(receiverName);
                order.setReceiverAddress(receiverAddress);
                order.setReceiverPhone(receiverPhone);
                order.setStatus("1PENDING");

                double sum = 0;
                for (CartDetail cd : cartDetails) {
                    if (cd.getCheckbox() != 0) {
                        sum += cd.getPrice() * cd.getQuantity();
                    }

                }
                order.setTotalPrice(sum);
                order = this.orderRepository.save(order);

                for (CartDetail cd : cartDetails) {
                    if (cd.getCheckbox() != 0) {
                        OrderDetail orderDetail = new OrderDetail();
                        orderDetail.setOrder(order);
                        orderDetail.setProduct(cd.getProduct());
                        orderDetail.setPrice(cd.getPrice());
                        orderDetail.setQuantity(cd.getQuantity());

                        this.orderDetailRepository.save(orderDetail);
                    }

                }
                int i = 0;
                // step 2: delete cart detail and cart
                for (CartDetail cd : cartDetails) {
                    if (cd.getCheckbox() != 0) {
                        this.cartDetailRepository.deleteById(cd.getId());
                    } else {
                        i++;
                    }
                }

                try {
                    if (i == 0) {
                        this.cartRepository.deleteById(cart.getId());
                    }
                } catch (Exception e) {
                    // TODO: handle exception
                }

                // step 3: update session
                session.setAttribute("sum", i);

            }
        }
    }

    public long countProducts() {
        return this.productRepository.count();
    }

}
