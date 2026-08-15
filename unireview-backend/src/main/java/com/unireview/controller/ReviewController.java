package com.unireview.controller;

import com.unireview.dto.request.ReportRequest;
import com.unireview.dto.request.ReviewCreateRequest;
import com.unireview.dto.request.VoteRequest;
import com.unireview.dto.response.ReviewSubmitResponse;
import com.unireview.exception.CaptchaFailedException;
import com.unireview.service.CaptchaService;
import com.unireview.service.ReviewService;
import com.unireview.util.IpHashUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews API", description = "Submit reviews, vote, and report reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final CaptchaService captchaService;

    @PostMapping
    @Operation(summary = "Submit a new review (runs AI moderation -> instant credit if approved)")
    public ResponseEntity<ReviewSubmitResponse> submitReview(
            @Valid @RequestBody ReviewCreateRequest request,
            @RequestHeader(value = "X-Reviewer-Token", required = false) String reviewerToken,
            @CookieValue(value = "reviewer_token", required = false) String cookieToken,
            HttpServletRequest httpServletRequest
    ) {
        String token = (reviewerToken != null && !reviewerToken.isBlank()) ? reviewerToken : cookieToken;
        String clientIp = getClientIp(httpServletRequest);

        ReviewSubmitResponse response = reviewService.submitReview(request, token, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/vote")
    @Operation(summary = "Upvote or Downvote a review")
    public ResponseEntity<Void> voteReview(
            @PathVariable Long id,
            @Valid @RequestBody VoteRequest request,
            @RequestHeader(value = "X-Reviewer-Token", required = false) String reviewerToken,
            @CookieValue(value = "reviewer_token", required = false) String cookieToken
    ) {
        if (!captchaService.verify(request.getCaptchaToken())) {
            throw new CaptchaFailedException("Xác thực reCAPTCHA không thành công");
        }
        String token = (reviewerToken != null && !reviewerToken.isBlank()) ? reviewerToken : cookieToken;
        reviewService.voteReview(id, request, token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/report")
    @Operation(summary = "Report a review for content violation")
    public ResponseEntity<Void> reportReview(
            @PathVariable Long id,
            @Valid @RequestBody ReportRequest request,
            @RequestHeader(value = "X-Reviewer-Token", required = false) String reviewerToken,
            @CookieValue(value = "reviewer_token", required = false) String cookieToken
    ) {
        if (!captchaService.verify(request.getCaptchaToken())) {
            throw new CaptchaFailedException("Xác thực reCAPTCHA không thành công");
        }
        String token = (reviewerToken != null && !reviewerToken.isBlank()) ? reviewerToken : cookieToken;
        reviewService.reportReview(id, request, token);
        return ResponseEntity.ok().build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        String rawIp = (xfHeader == null) ? request.getRemoteAddr() : xfHeader.split(",")[0];
        return IpHashUtil.hash(rawIp);
    }
}
