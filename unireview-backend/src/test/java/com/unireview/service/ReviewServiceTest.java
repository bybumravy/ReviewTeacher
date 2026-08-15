package com.unireview.service;

import com.unireview.dto.request.ReportRequest;
import com.unireview.dto.request.ReviewCreateRequest;
import com.unireview.dto.request.VoteRequest;
import com.unireview.entity.Review;
import com.unireview.entity.ReviewVote;
import com.unireview.entity.Reviewer;
import com.unireview.entity.Teacher;
import com.unireview.enums.*;
import com.unireview.exception.DuplicateReviewException;
import com.unireview.exception.NoReviewerTokenException;
import com.unireview.exception.RateLimitExceededException;
import com.unireview.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReviewServiceTest {

    private ReviewRepository reviewRepository;
    private TeacherRepository teacherRepository;
    private SubjectRepository subjectRepository;
    private ReviewerRepository reviewerRepository;
    private ReviewVoteRepository reviewVoteRepository;
    private ReviewReportRepository reviewReportRepository;
    private GateService gateService;
    private ModerationService moderationService;
    private CaptchaService captchaService;
    private ReviewService reviewService;

    private static final Long TEACHER_ID = 1L;
    private static final String TOKEN = "rv_test_token";

    @BeforeEach
    void setUp() {
        reviewRepository = mock(ReviewRepository.class);
        teacherRepository = mock(TeacherRepository.class);
        subjectRepository = mock(SubjectRepository.class);
        reviewerRepository = mock(ReviewerRepository.class);
        reviewVoteRepository = mock(ReviewVoteRepository.class);
        reviewReportRepository = mock(ReviewReportRepository.class);
        gateService = mock(GateService.class);
        moderationService = mock(ModerationService.class);
        captchaService = mock(CaptchaService.class);

        reviewService = new ReviewService(
                reviewRepository, teacherRepository, subjectRepository, reviewerRepository,
                reviewVoteRepository, reviewReportRepository, gateService, moderationService, captchaService
        );

        when(captchaService.verify(any())).thenReturn(true);
        when(reviewRepository.countByIpHashAndCreatedAtAfter(anyString(), any(LocalDateTime.class))).thenReturn(0);

        Teacher teacher = Teacher.builder().id(TEACHER_ID).fullName("Nguyen Van A").faculty("CNTT").build();
        when(teacherRepository.findById(TEACHER_ID)).thenReturn(java.util.Optional.of(teacher));

        Reviewer reviewer = Reviewer.builder().id(1L).token(TOKEN).creditBalance(0).reviewCount(0).build();
        when(gateService.getOrCreateReviewer(any(), any())).thenReturn(reviewer);

        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        when(reviewRepository.calculateAvgRatingForTeacher(TEACHER_ID)).thenReturn(4.5);
        when(reviewRepository.countApprovedReviewsForTeacher(TEACHER_ID)).thenReturn(1);
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private ReviewCreateRequest validRequest() {
        ReviewCreateRequest req = new ReviewCreateRequest();
        req.setTeacherId(TEACHER_ID);
        req.setRatingOverall(5);
        req.setRatingTeaching(5);
        req.setRatingGrading(5);
        req.setRatingPersonality(5);
        req.setDifficulty(Difficulty.EASY);
        req.setAttendance(Attendance.SOMETIMES);
        req.setMaterialsAllowed(MaterialsAllowed.YES);
        req.setWouldRecommend(Recommendation.YES);
        req.setWorkload(Workload.LIGHT);
        req.setContent("Thầy dạy rất nhiệt tình và dễ hiểu, mình học được rất nhiều điều bổ ích từ môn này.");
        req.setSemester("HK1 2025-2026");
        req.setCaptchaToken("token");
        return req;
    }

    @Test
    void submitReview_duplicateReview_throwsException() {
        when(reviewRepository.existsByReviewerTokenAndTeacherId(TOKEN, TEACHER_ID)).thenReturn(true);

        assertThrows(DuplicateReviewException.class,
                () -> reviewService.submitReview(validRequest(), TOKEN, "hashed-ip"));

        verify(moderationService, never()).evaluateContent(any());
    }

    @Test
    void submitReview_approved_awardsCreditAndAutoUnlocks() {
        when(reviewRepository.existsByReviewerTokenAndTeacherId(TOKEN, TEACHER_ID)).thenReturn(false);
        when(moderationService.evaluateContent(anyString()))
                .thenReturn(new ModerationService.ModerationResult(ReviewStatus.APPROVED, BigDecimal.valueOf(0.1)));

        var response = reviewService.submitReview(validRequest(), TOKEN, "hashed-ip");

        assertEquals(ReviewStatus.APPROVED, response.getStatus());
        assertEquals(TEACHER_ID, response.getAutoUnlockedTeacherId());
        verify(gateService).addCreditAndUnlock(TOKEN, TEACHER_ID);
    }

    @Test
    void submitReview_flagged_noCreditNoAutoUnlock() {
        when(reviewRepository.existsByReviewerTokenAndTeacherId(TOKEN, TEACHER_ID)).thenReturn(false);
        when(moderationService.evaluateContent(anyString()))
                .thenReturn(new ModerationService.ModerationResult(ReviewStatus.FLAGGED, BigDecimal.valueOf(0.85)));

        var response = reviewService.submitReview(validRequest(), TOKEN, "hashed-ip");

        assertEquals(ReviewStatus.FLAGGED, response.getStatus());
        assertNull(response.getAutoUnlockedTeacherId());
        verify(gateService, never()).addCreditAndUnlock(any(), any());
    }

    @Test
    void submitReview_rateLimitExceeded_throwsBeforeModeration() {
        when(reviewRepository.countByIpHashAndCreatedAtAfter(anyString(), any(LocalDateTime.class))).thenReturn(3);

        assertThrows(RateLimitExceededException.class,
                () -> reviewService.submitReview(validRequest(), TOKEN, "hashed-ip"));

        verify(moderationService, never()).evaluateContent(any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void submitReview_persistsWhateverIpHashItIsGiven_neverReHashesOrStripsIt() {
        // ReviewService trusts the controller has already hashed the IP (see IpHashUtilTest for the
        // hashing invariant itself); this asserts the service doesn't accidentally store a raw value
        // when handed an already-hashed one.
        when(reviewRepository.existsByReviewerTokenAndTeacherId(TOKEN, TEACHER_ID)).thenReturn(false);
        when(moderationService.evaluateContent(anyString()))
                .thenReturn(new ModerationService.ModerationResult(ReviewStatus.APPROVED, BigDecimal.valueOf(0.1)));
        String preHashedIp = com.unireview.util.IpHashUtil.hash("198.51.100.7");

        reviewService.submitReview(validRequest(), TOKEN, preHashedIp);

        verify(reviewRepository).save(argThat(r -> preHashedIp.equals(r.getIpHash()) && !r.getIpHash().equals("198.51.100.7")));
    }

    // ---- US4: voteReview ----

    @Test
    void voteReview_noVoterToken_throwsNoReviewerToken() {
        VoteRequest req = new VoteRequest();
        req.setVoteType(VoteType.UPVOTE);

        assertThrows(NoReviewerTokenException.class, () -> reviewService.voteReview(100L, req, null));
        assertThrows(NoReviewerTokenException.class, () -> reviewService.voteReview(100L, req, "  "));
        verify(reviewRepository, never()).findById(any());
    }

    @Test
    void voteReview_firstUpvote_incrementsUpvoteCount() {
        Review review = Review.builder().id(100L).upvoteCount(0).downvoteCount(0).build();
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));
        when(reviewVoteRepository.findByVoterTokenAndReviewId(TOKEN, 100L)).thenReturn(Optional.empty());
        VoteRequest req = new VoteRequest();
        req.setVoteType(VoteType.UPVOTE);

        reviewService.voteReview(100L, req, TOKEN);

        assertEquals(1, review.getUpvoteCount());
        assertEquals(0, review.getDownvoteCount());
        verify(reviewVoteRepository).save(any(ReviewVote.class));
    }

    @Test
    void voteReview_repeatSameType_isNoOp() {
        Review review = Review.builder().id(100L).upvoteCount(1).downvoteCount(0).build();
        ReviewVote existing = ReviewVote.builder().id(1L).voterToken(TOKEN).review(review).voteType(VoteType.UPVOTE).build();
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));
        when(reviewVoteRepository.findByVoterTokenAndReviewId(TOKEN, 100L)).thenReturn(Optional.of(existing));
        VoteRequest req = new VoteRequest();
        req.setVoteType(VoteType.UPVOTE);

        reviewService.voteReview(100L, req, TOKEN);

        assertEquals(1, review.getUpvoteCount());
        verify(reviewVoteRepository, never()).save(any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void voteReview_switchVoteType_movesCountsBetweenColumns() {
        Review review = Review.builder().id(100L).upvoteCount(1).downvoteCount(0).build();
        ReviewVote existing = ReviewVote.builder().id(1L).voterToken(TOKEN).review(review).voteType(VoteType.UPVOTE).build();
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));
        when(reviewVoteRepository.findByVoterTokenAndReviewId(TOKEN, 100L)).thenReturn(Optional.of(existing));
        VoteRequest req = new VoteRequest();
        req.setVoteType(VoteType.DOWNVOTE);

        reviewService.voteReview(100L, req, TOKEN);

        assertEquals(0, review.getUpvoteCount());
        assertEquals(1, review.getDownvoteCount());
        assertEquals(VoteType.DOWNVOTE, existing.getVoteType());
    }

    // ---- US4: reportReview ----

    @Test
    void reportReview_noReporterToken_throwsNoReviewerToken() {
        ReportRequest req = new ReportRequest();
        req.setReason("Spam");

        assertThrows(NoReviewerTokenException.class, () -> reviewService.reportReview(100L, req, null));
        assertThrows(NoReviewerTokenException.class, () -> reviewService.reportReview(100L, req, ""));
        verify(reviewReportRepository, never()).save(any());
    }

    @Test
    void reportReview_withToken_persistsReporterToken() {
        Review review = Review.builder().id(100L).build();
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));
        ReportRequest req = new ReportRequest();
        req.setReason("Spam");
        req.setDescription("Chứa link quảng cáo");

        reviewService.reportReview(100L, req, TOKEN);

        verify(reviewReportRepository).save(argThat(report -> TOKEN.equals(report.getReporterToken())));
    }
}
