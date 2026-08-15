package com.unireview.repository;

import com.unireview.entity.ReviewReport;
import com.unireview.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {
    Page<ReviewReport> findByStatus(ReportStatus status, Pageable pageable);

    List<ReviewReport> findByReviewIdAndStatus(Long reviewId, ReportStatus status);
}
