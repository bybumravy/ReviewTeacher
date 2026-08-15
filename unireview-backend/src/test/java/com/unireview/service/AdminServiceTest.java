package com.unireview.service;

import com.unireview.entity.Review;
import com.unireview.entity.ReviewReport;
import com.unireview.entity.Reviewer;
import com.unireview.entity.Teacher;
import com.unireview.enums.ReportStatus;
import com.unireview.enums.ReviewStatus;
import com.unireview.repository.*;
import com.unireview.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminServiceTest {

    private AdminUserRepository adminUserRepository;
    private ReviewRepository reviewRepository;
    private ReviewReportRepository reviewReportRepository;
    private ReviewerRepository reviewerRepository;
    private GateService gateService;
    private ReviewService reviewService;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private AdminService adminService;

    private static final Long REVIEW_ID = 100L;
    private static final Long TEACHER_ID = 1L;
    private static final String TOKEN = "rv_author_token";

    @BeforeEach
    void setUp() {
        adminUserRepository = mock(AdminUserRepository.class);
        reviewRepository = mock(ReviewRepository.class);
        reviewReportRepository = mock(ReviewReportRepository.class);
        reviewerRepository = mock(ReviewerRepository.class);
        gateService = mock(GateService.class);
        reviewService = mock(ReviewService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);

        adminService = new AdminService(
                adminUserRepository, reviewRepository, reviewReportRepository, reviewerRepository,
                gateService, reviewService, passwordEncoder, jwtTokenProvider
        );
    }

    private Review reviewWithTeacher() {
        Teacher teacher = Teacher.builder().id(TEACHER_ID).fullName("A").faculty("CNTT").build();
        return Review.builder().id(REVIEW_ID).reviewerToken(TOKEN).teacher(teacher).status(ReviewStatus.APPROVED).build();
    }

    @Test
    void hideReview_setsStatusHidden() {
        Review review = reviewWithTeacher();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewerRepository.findByToken(TOKEN)).thenReturn(Optional.empty());
        when(reviewReportRepository.findByReviewIdAndStatus(REVIEW_ID, ReportStatus.PENDING)).thenReturn(List.of());

        adminService.hideReview(REVIEW_ID);

        assertEquals(ReviewStatus.HIDDEN, review.getStatus());
        verify(reviewRepository).save(review);
        verify(reviewService).recalculateTeacherRating(TEACHER_ID);
    }

    @Test
    void hideReview_deductsCreditOnlyWhenBalancePositive() {
        Review review = reviewWithTeacher();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        Reviewer reviewer = Reviewer.builder().id(1L).token(TOKEN).creditBalance(2).build();
        when(reviewerRepository.findByToken(TOKEN)).thenReturn(Optional.of(reviewer));
        when(reviewReportRepository.findByReviewIdAndStatus(REVIEW_ID, ReportStatus.PENDING)).thenReturn(List.of());

        adminService.hideReview(REVIEW_ID);

        assertEquals(1, reviewer.getCreditBalance());
        verify(reviewerRepository).save(reviewer);
    }

    @Test
    void hideReview_zeroBalance_doesNotGoNegative() {
        Review review = reviewWithTeacher();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        Reviewer reviewer = Reviewer.builder().id(1L).token(TOKEN).creditBalance(0).build();
        when(reviewerRepository.findByToken(TOKEN)).thenReturn(Optional.of(reviewer));
        when(reviewReportRepository.findByReviewIdAndStatus(REVIEW_ID, ReportStatus.PENDING)).thenReturn(List.of());

        adminService.hideReview(REVIEW_ID);

        assertEquals(0, reviewer.getCreditBalance());
        verify(reviewerRepository, never()).save(any());
    }

    @Test
    void hideReview_resolvesAllPendingReportsForThatReview() {
        Review review = reviewWithTeacher();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewerRepository.findByToken(TOKEN)).thenReturn(Optional.empty());
        ReviewReport report1 = ReviewReport.builder().id(1L).review(review).status(ReportStatus.PENDING).build();
        ReviewReport report2 = ReviewReport.builder().id(2L).review(review).status(ReportStatus.PENDING).build();
        when(reviewReportRepository.findByReviewIdAndStatus(REVIEW_ID, ReportStatus.PENDING))
                .thenReturn(List.of(report1, report2));

        adminService.hideReview(REVIEW_ID);

        assertEquals(ReportStatus.RESOLVED, report1.getStatus());
        assertEquals(ReportStatus.RESOLVED, report2.getStatus());
        verify(reviewReportRepository).saveAll(List.of(report1, report2));
    }

    @Test
    void getReports_defaultsToPendingStatus() {
        Page<ReviewReport> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(reviewReportRepository.findByStatus(ReportStatus.PENDING, PageRequest.of(0, 10))).thenReturn(page);

        adminService.getReports(null, 0, 10);

        verify(reviewReportRepository).findByStatus(ReportStatus.PENDING, PageRequest.of(0, 10));
    }

    @Test
    void getReports_returnsMappedContentAndPaginationMetadata() {
        Review review = reviewWithTeacher();
        review.setContent("Nội dung review");
        ReviewReport report = ReviewReport.builder().id(1L).review(review).reason("Spam").status(ReportStatus.PENDING).build();
        Page<ReviewReport> page = new PageImpl<>(List.of(report), PageRequest.of(0, 10), 1);
        when(reviewReportRepository.findByStatus(ReportStatus.PENDING, PageRequest.of(0, 10))).thenReturn(page);

        var response = adminService.getReports(ReportStatus.PENDING, 0, 10);

        assertEquals(1, response.getContent().size());
        assertEquals(REVIEW_ID, response.getContent().get(0).getReviewId());
        assertEquals("Spam", response.getContent().get(0).getReason());
        assertEquals(1, response.getTotalElements());
    }

    @Test
    void dismissReport_setsStatusDismissed() {
        ReviewReport report = ReviewReport.builder().id(1L).status(ReportStatus.PENDING).build();
        when(reviewReportRepository.findById(1L)).thenReturn(Optional.of(report));

        adminService.dismissReport(1L);

        assertEquals(ReportStatus.DISMISSED, report.getStatus());
        verify(reviewReportRepository).save(report);
    }
}
