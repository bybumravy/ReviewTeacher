package com.unireview.controller;

import com.unireview.dto.response.PagedResponse;
import com.unireview.dto.response.ReviewResponse;
import com.unireview.dto.response.TeacherDetailResponse;
import com.unireview.dto.response.TeacherResponse;
import com.unireview.service.ReviewService;
import com.unireview.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
@Tag(name = "Teachers API", description = "Endpoints for listing teachers, details, and gated reviews")
public class TeacherController {

    private final TeacherService teacherService;
    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "Get paginated teachers list with search and filter parameters")
    public ResponseEntity<PagedResponse<TeacherResponse>> getTeachers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String faculty,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false, defaultValue = "name") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        PagedResponse<TeacherResponse> response = teacherService.getTeachers(
                search, faculty, minRating, sortBy, sortDir, page, size
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get teacher basic details, star distribution, and public aggregate stats")
    public ResponseEntity<TeacherDetailResponse> getTeacherDetail(@PathVariable Long id) {
        return ResponseEntity.ok(teacherService.getTeacherDetail(id));
    }

    @GetMapping("/{id}/reviews")
    @Operation(summary = "Get detailed reviews for a teacher (Requires Reviewer Cookie Token & Credit check)")
    public ResponseEntity<List<ReviewResponse>> getTeacherReviews(
            @PathVariable Long id,
            @RequestHeader(value = "X-Reviewer-Token", required = false) String reviewerToken,
            @CookieValue(value = "reviewer_token", required = false) String cookieToken
    ) {
        String token = (reviewerToken != null && !reviewerToken.isBlank()) ? reviewerToken : cookieToken;
        List<ReviewResponse> reviews = reviewService.getReviewsForTeacher(id, token);
        return ResponseEntity.ok(reviews);
    }
}
