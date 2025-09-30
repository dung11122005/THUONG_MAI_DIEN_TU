package com.example.tmdt.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.tmdt.domain.Comment;




@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByProduct_Id(Long id);

    @Query(value = "SELECT * FROM cmt WHERE product_id = :productId", nativeQuery = true)
    List<Comment> findByProductId(@Param("productId") Long productId);

}
