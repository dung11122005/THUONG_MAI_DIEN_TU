package com.example.exam_portal.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.example.exam_portal.domain.Course;


@Repository
public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course>{
    Course save(Course hoidanit);

    List<Course> findAll();

    List<Course> findByTeacherId(Long id);

    Page<Course> findByTeacherId(Long id, Pageable pageable);

    Course findById(long id);

    List<Course> findByIdIn(List<Long> ids);
}
