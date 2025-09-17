package com.example.exam_portal.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.example.exam_portal.domain.Chapter;
import com.example.exam_portal.domain.Course;
import com.example.exam_portal.domain.CourseLesson;
import com.example.exam_portal.repository.ChapterRepository;
import com.example.exam_portal.repository.CourseLessonRepository;
import com.example.exam_portal.repository.CourseRepository;

@Service
public class CourseService {
    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final CourseLessonRepository courseLessonRepository;

    public CourseService(CourseRepository courseRepository, ChapterRepository chapterRepository,
    CourseLessonRepository courseLessonRepository){
        this.courseRepository=courseRepository;
        this.chapterRepository=chapterRepository;
        this.courseLessonRepository=courseLessonRepository;
    }

    public Page<Course> getAllCoursePaginationTeacherId(Long id, Pageable page) {
        return this.courseRepository.findByTeacherId(id, page);
    }

    public Page<Course> getAllCoursePagination(Pageable page) {
        return this.courseRepository.findAll(page);
    }

    public Page<Course> getAllCoursePagination(Specification<Course> spec, Pageable pageable) {
        return this.courseRepository.findAll(spec, pageable);
    }

    public List<Course> getAllCourse() {
        return this.courseRepository.findAll();
    }

    public Course handleSaveCourse(Course course) {
        Course eric = this.courseRepository.save(course);
        return eric;
    }

    public Chapter handleSaveChapter(Chapter chapter) {
        Chapter eric = this.chapterRepository.save(chapter);
        return eric;
    }

    public CourseLesson handleSaveCourseLesson(CourseLesson chapter) {
        CourseLesson eric = this.courseLessonRepository.save(chapter);
        return eric;
    }

    public Optional<Chapter> getChapterById(long id) {
        return this.chapterRepository.findById(id);
    }

    public Optional<CourseLesson> getCourseLessonById(long id) {
        return this.courseLessonRepository.findById(id);
    }

    public Course getCourseById(long id) {
        return this.courseRepository.findById(id);
    }

    public Course getCourseBySlug(String slug) {
        return this.courseRepository.findBySlug(slug);
    }

    public List<Course> getCourseByTeacherId(long id) {
        return this.courseRepository.findByTeacherId(id);
    }

    public void deleteCourse(long id) {
        this.courseRepository.deleteById(id);
    }

    public void deleteChapter(long id) {
        this.chapterRepository.deleteById(id);
    }

    public void deleteCourseLesson(long id) {
        this.courseLessonRepository.deleteById(id);
    }
}
