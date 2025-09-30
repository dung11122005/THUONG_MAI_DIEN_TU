package com.example.tmdt.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.tmdt.domain.Comment;




@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByProduct_Id(Long id);
}
