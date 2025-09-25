package com.example.tmdt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.tmdt.domain.Comment;



@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

}
