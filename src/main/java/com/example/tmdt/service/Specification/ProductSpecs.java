package com.example.tmdt.service.Specification;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.example.tmdt.domain.Order;
import com.example.tmdt.domain.Order_;
import com.example.tmdt.domain.Product;
import com.example.tmdt.domain.Product_;



public class ProductSpecs {
    public static Specification<Product> nameLike(String name) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get(Product_.NAME), "%" + name + "%");
    }

    // case 1
    public static Specification<Product> minPrice(double price) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.ge(root.get(Product_.PRICE), price);
    }

    // case 2
    public static Specification<Product> maxPrice(double price) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.le(root.get(Product_.PRICE), price);
    }

    // case3
    public static Specification<Product> matchFactory(String factory) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Product_.FACTORY), factory);
    }

    // case4
    public static Specification<Product> matchListFactory(List<String> factory) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.in(root.get(Product_.FACTORY)).value(factory);
    }

    // case4
    public static Specification<Product> matchListTarget(List<String> target) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.in(root.get(Product_.TARGET)).value(target);
    }

    public static Specification<Product> matchListStar(String ValueStar) {
        int star = Integer.parseInt(ValueStar);
        return (root, query, criteriaBuilder) -> criteriaBuilder.in(root.get(Product_.STA)).value(star);
    }

    public static Specification<Product> matchListSearch(String valueSearch) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get(Product_.NAME), "%" + valueSearch + "%");
    }

    public static Specification<Order> matchListShipSearch(String valueShipSearch) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get(Order_.RECEIVER_ADDRESS),
                "%" + valueShipSearch + "%");
    }

    // case5
    public static Specification<Product> matchPrice(double min, double max) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.gt(root.get(Product_.PRICE), min),
                criteriaBuilder.le(root.get(Product_.PRICE), max));
    }

    // case6
    public static Specification<Product> matchMultiplePrice(double min, double max) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.between(
                root.get(Product_.PRICE), min, max);
    }
}
