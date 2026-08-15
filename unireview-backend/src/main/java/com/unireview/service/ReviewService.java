package com.unireview.service;

import com.unireview.dto.request.ReportRequest;
import com.unireview.dto.request.ReviewCreateRequest;
import com.unireview.dto.request.VoteRequest;
import com.unireview.dto.response.ReviewResponse;
import com.unireview.dto.response.ReviewSubmitResponse;
import com.unireview.entity.*;
import com.unireview.enums.ReportStatus;
import com.unireview.enums.ReviewStatus;
import com.unireview.enums.VoteType;
import com.unireview.exception.*;
import com.unireview.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final int MAX_REVIEWS_PER_IP_PER_DAY = 3;

    private final ReviewRepository reviewRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final ReviewerRepository reviewerRepository;
    private final ReviewVoteRepository reviewVoteRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final GateService gateService;
    private final ModerationService moderationService;
    private final CaptchaService captchaService;

    @Transactional
    public ReviewSubmitResponse submitReview(ReviewCreateRequest req, String reviewerToken, String clientIp) {
        // 1. Verify Captcha
        if (!captchaService.verify(req.getCaptchaToken())) {
            throw new CaptchaFailedException("Xác thực reCAPTCHA không thành công");
        }

        // 2. Rate limit by IP (max 3 reviews/day)
        if (clientIp != null) {
            int recentCount = reviewRepository.countByIpHashAndCreatedAtAfter(clientIp, LocalDateTime.now().minusDays(1));
            if (recentCount >= MAX_REVIEWS_PER_IP_PER_DAY) {
                throw new RateLimitExceededException("Bạn đã gửi quá " + MAX_REVIEWS_PER_IP_PER_DAY + " review trong hôm nay, vui lòng thử lại vào ngày mai.");
            }
        }

        // 3. Fetch Teacher
        Teacher teacher = teacherRepository.findById(req.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giảng viên ID: " + req.getTeacherId()));

        // 4. Ensure Reviewer Entity exists
        Reviewer reviewer = gateService.getOrCreateReviewer(reviewerToken, clientIp);
        String finalToken = reviewer.getToken();

        // 5. Check duplicate review
        if (reviewRepository.existsByReviewerTokenAndTeacherId(finalToken, req.getTeacherId())) {
            throw new DuplicateReviewException("Bạn đã gửi đánh giá cho giảng viên này rồi");
        }

        // 6. Evaluate content (AI Moderation)
        ModerationService.ModerationResult modResult = moderationService.evaluateContent(req.getContent());

        if (modResult.status() == ReviewStatus.REJECTED) {
            throw new ContentViolationException("Nội dung đánh giá chứa từ ngữ vi phạm, SĐT hoặc link quảng cáo");
        }

        Subject subject = null;
        if (req.getSubjectId() != null) {
            subject = subjectRepository.findById(req.getSubjectId()).orElse(null);
        }

        // 6. Save Review
        Review review = Review.builder()
                .reviewerToken(finalToken)
                .teacher(teacher)
                .subject(subject)
                .ratingOverall(req.getRatingOverall())
                .ratingTeaching(req.getRatingTeaching())
                .ratingGrading(req.getRatingGrading())
                .ratingPersonality(req.getRatingPersonality())
                .difficulty(req.getDifficulty())
                .attendance(req.getAttendance())
                .materialsAllowed(req.getMaterialsAllowed())
                .wouldRecommend(req.getWouldRecommend())
                .workload(req.getWorkload())
                .content(req.getContent())
                .semester(req.getSemester())
                .status(modResult.status())
                .toxicityScore(modResult.toxicityScore())
                .ipHash(clientIp)
                .build();

        review = reviewRepository.save(review);

        String message;
        Long autoUnlockedTeacherId = null;

        // 7. If APPROVED: Add credit & auto-unlock teacher
        if (modResult.status() == ReviewStatus.APPROVED) {
            gateService.addCreditAndUnlock(finalToken, teacher.getId());
            recalculateTeacherRating(teacher.getId());
            message = "Review đã được đăng thành công! +1 credit 🎉";
            autoUnlockedTeacherId = teacher.getId();
        } else {
            message = "Review đang được kiểm duyệt. Credit sẽ được cộng sau khi xác nhận.";
        }

        return ReviewSubmitResponse.builder()
                .id(review.getId())
                .status(review.getStatus())
                .message(message)
                .reviewerToken(finalToken)
                .creditBalance(reviewer.getCreditBalance())
                .autoUnlockedTeacherId(autoUnlockedTeacherId)
                .build();
    }

    @Transactional
    public List<ReviewResponse> getReviewsForTeacher(Long teacherId, String reviewerToken) {
        // Gate check
        gateService.checkAndUnlockTeacher(reviewerToken, teacherId);

        return reviewRepository.findByTeacherIdAndStatusOrderByCreatedAtDesc(teacherId, ReviewStatus.APPROVED)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void voteReview(Long reviewId, VoteRequest req, String voterToken) {
        if (voterToken == null || voterToken.isBlank()) {
            throw new NoReviewerTokenException("Cần có định danh người dùng để bình chọn");
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy review ID: " + reviewId));

        Optional<ReviewVote> existingOpt = reviewVoteRepository.findByVoterTokenAndReviewId(voterToken, reviewId);

        if (existingOpt.isPresent()) {
            ReviewVote existing = existingOpt.get();
            if (existing.getVoteType() == req.getVoteType()) {
                return; // Already voted same type
            }
            // Switch vote type
            if (req.getVoteType() == VoteType.UPVOTE) {
                review.setUpvoteCount(review.getUpvoteCount() + 1);
                review.setDownvoteCount(Math.max(0, review.getDownvoteCount() - 1));
            } else {
                review.setDownvoteCount(review.getDownvoteCount() + 1);
                review.setUpvoteCount(Math.max(0, review.getUpvoteCount() - 1));
            }
            existing.setVoteType(req.getVoteType());
            reviewVoteRepository.save(existing);
        } else {
            ReviewVote vote = ReviewVote.builder()
                    .voterToken(voterToken)
                    .review(review)
                    .voteType(req.getVoteType())
                    .build();
            reviewVoteRepository.save(vote);

            if (req.getVoteType() == VoteType.UPVOTE) {
                review.setUpvoteCount(review.getUpvoteCount() + 1);
            } else {
                review.setDownvoteCount(review.getDownvoteCount() + 1);
            }
        }

        reviewRepository.save(review);
    }

    @Transactional
    public void reportReview(Long reviewId, ReportRequest req, String reporterToken) {
        if (reporterToken == null || reporterToken.isBlank()) {
            throw new NoReviewerTokenException("Cần có định danh người dùng để báo cáo");
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy review ID: " + reviewId));

        ReviewReport report = ReviewReport.builder()
                .review(review)
                .reporterToken(reporterToken)
                .reason(req.getReason())
                .description(req.getDescription())
                .status(ReportStatus.PENDING)
                .build();

        reviewReportRepository.save(report);
    }

    @Transactional
    public void recalculateTeacherRating(Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId).orElse(null);
        if (teacher == null) return;

        Double avg = reviewRepository.calculateAvgRatingForTeacher(teacherId);
        int total = reviewRepository.countApprovedReviewsForTeacher(teacherId);

        teacher.setAvgRating(avg != null ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        teacher.setTotalReviews(total);
        teacherRepository.save(teacher);
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
