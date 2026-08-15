package com.unireview.repository;

import com.unireview.entity.Review;
import com.unireview.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByTeacherIdAndStatusOrderByCreatedAtDesc(Long teacherId, ReviewStatus status);

    boolean existsByReviewerTokenAndTeacherId(String reviewerToken, Long teacherId);

    int countByIpHashAndCreatedAtAfter(String ipHash, LocalDateTime cutoff);

    int countByReviewerTokenAndStatus(String reviewerToken, ReviewStatus status);

    Page<Review> findByStatus(ReviewStatus status, Pageable pageable);

    @Query("SELECT AVG(r.ratingOverall) FROM Review r WHERE r.teacher.id = :teacherId AND r.status = 'APPROVED'")
    Double calculateAvgRatingForTeacher(@Param("teacherId") Long teacherId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.teacher.id = :teacherId AND r.status = 'APPROVED'")
    int countApprovedReviewsForTeacher(@Param("teacherId") Long teacherId);
}
