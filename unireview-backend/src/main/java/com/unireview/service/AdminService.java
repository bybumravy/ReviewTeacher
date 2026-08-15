package com.unireview.service;

import com.unireview.dto.request.AdminLoginRequest;
import com.unireview.dto.response.PagedResponse;
import com.unireview.dto.response.ReportResponse;
import com.unireview.dto.response.ReviewResponse;
import com.unireview.entity.AdminUser;
import com.unireview.entity.Review;
import com.unireview.entity.ReviewReport;
import com.unireview.entity.Reviewer;
import com.unireview.enums.ReportStatus;
import com.unireview.enums.ReviewStatus;
import com.unireview.exception.ResourceNotFoundException;
import com.unireview.repository.AdminUserRepository;
import com.unireview.repository.ReviewReportRepository;
import com.unireview.repository.ReviewRepository;
import com.unireview.repository.ReviewerRepository;
import com.unireview.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminUserRepository adminUserRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final ReviewerRepository reviewerRepository;
    private final GateService gateService;
    private final ReviewService reviewService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public Map<String, String> login(AdminLoginRequest req) {
        AdminUser admin = adminUserRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Tên đăng nhập hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(req.getPassword(), admin.getPasswordHash())) {
            throw new ResourceNotFoundException("Tên đăng nhập hoặc mật khẩu không đúng");
        }

        String token = jwtTokenProvider.generateToken(admin.getUsername(), admin.getRole());
        return Map.of("token", token, "username", admin.getUsername(), "role", admin.getRole());
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getFlaggedReviews(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviewPage = reviewRepository.findByStatus(ReviewStatus.FLAGGED, pageable);

        List<ReviewResponse> content = reviewPage.getContent().stream().map(this::mapToResponse).toList();

        return PagedResponse.<ReviewResponse>builder()
                .content(content)
                .page(reviewPage.getNumber())
                .size(reviewPage.getSize())
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .last(reviewPage.isLast())
                .build();
    }

    @Transactional
    public void approveReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy review ID: " + reviewId));

        review.setStatus(ReviewStatus.APPROVED);
        reviewRepository.save(review);

        // Add credit & auto unlock
        gateService.addCreditAndUnlock(review.getReviewerToken(), review.getTeacher().getId());
        reviewService.recalculateTeacherRating(review.getTeacher().getId());
    }

    @Transactional
    public void rejectReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy review ID: " + reviewId));

        review.setStatus(ReviewStatus.REJECTED);
        reviewRepository.save(review);
    }

    @Transactional
    public void hideReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy review ID: " + reviewId));

        review.setStatus(ReviewStatus.HIDDEN);
        reviewRepository.save(review);

        reviewerRepository.findByToken(review.getReviewerToken()).ifPresent(reviewer -> {
            if (reviewer.getCreditBalance() > 0) {
                reviewer.setCreditBalance(reviewer.getCreditBalance() - 1);
                reviewerRepository.save(reviewer);
            }
        });

        reviewService.recalculateTeacherRating(review.getTeacher().getId());

        List<ReviewReport> pendingReports = reviewReportRepository.findByReviewIdAndStatus(reviewId, ReportStatus.PENDING);
        pendingReports.forEach(report -> report.setStatus(ReportStatus.RESOLVED));
        reviewReportRepository.saveAll(pendingReports);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReportResponse> getReports(ReportStatus status, int page, int size) {
        ReportStatus effectiveStatus = status != null ? status : ReportStatus.PENDING;
        Pageable pageable = PageRequest.of(page, size);
        Page<ReviewReport> reportPage = reviewReportRepository.findByStatus(effectiveStatus, pageable);

        List<ReportResponse> content = reportPage.getContent().stream().map(this::mapToReportResponse).toList();

        return PagedResponse.<ReportResponse>builder()
                .content(content)
                .page(reportPage.getNumber())
                .size(reportPage.getSize())
                .totalElements(reportPage.getTotalElements())
                .totalPages(reportPage.getTotalPages())
                .last(reportPage.isLast())
                .build();
    }

    @Transactional
    public void dismissReport(Long reportId) {
        ReviewReport report = reviewReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo cáo ID: " + reportId));

        report.setStatus(ReportStatus.DISMISSED);
        reviewReportRepository.save(report);
    }

    private ReportResponse mapToReportResponse(ReviewReport r) {
        return ReportResponse.builder()
                .id(r.getId())
                .reviewId(r.getReview().getId())
                .reviewContent(r.getReview().getContent())
                .reason(r.getReason())
                .description(r.getDescription())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private ReviewResponse mapToResponse(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .teacherId(r.getTeacher().getId())
                .teacherName(r.getTeacher().getFullName())
                .ratingOverall(r.getRatingOverall())
                .ratingTeaching(r.getRatingTeaching())
                .ratingGrading(r.getRatingGrading())
                .ratingPersonality(r.getRatingPersonality())
                .difficulty(r.getDifficulty())
                .attendance(r.getAttendance())
                .materialsAllowed(r.getMaterialsAllowed())
                .wouldRecommend(r.getWouldRecommend())
                .workload(r.getWorkload())
                .content(r.getContent())
                .semester(r.getSemester())
                .upvoteCount(r.getUpvoteCount())
                .downvoteCount(r.getDownvoteCount())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
