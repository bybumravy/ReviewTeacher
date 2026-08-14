package com.unireview.service;

import com.unireview.dto.request.AdminLoginRequest;
import com.unireview.dto.response.PagedResponse;
import com.unireview.dto.response.ReviewResponse;
import com.unireview.entity.AdminUser;
import com.unireview.entity.Review;
import com.unireview.enums.ReviewStatus;
import com.unireview.exception.ResourceNotFoundException;
import com.unireview.repository.AdminUserRepository;
import com.unireview.repository.ReviewRepository;
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
