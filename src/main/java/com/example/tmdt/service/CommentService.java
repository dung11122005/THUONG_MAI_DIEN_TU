package com.example.tmdt.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.tmdt.domain.Comment;
import com.example.tmdt.domain.Product;
import com.example.tmdt.domain.User;
import com.example.tmdt.repository.CommentRepository;
import com.example.tmdt.repository.ProductRepository;



@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final ProductRepository productRepository;

    public CommentService(CommentRepository commentRepository, ProductRepository productRepository) {
        this.commentRepository = commentRepository;
        this.productRepository = productRepository;
    }

    public void handleConfirmComment(int star, String description, long idProduct, User currentUser,
            Product product) {

        try {
            List<Comment> cOptional = product.getComments();
            if (cOptional != null) {
                int dem = 0;
                int tong = 0;
                for (Comment cmt : cOptional) {
                    tong = cmt.getSta() + tong;
                    dem++;
                }
                float num = (float) (tong) / (float) dem;
                int starProduct = Math.round(num);
                product.setSta(starProduct);
                this.productRepository.save(product);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }

        Comment comment = new Comment();

        comment.setSta(star);
        comment.setDescription(description);
        comment.setUser(currentUser);
        comment.setProduct(product);
        this.commentRepository.save(comment);
    }

}
