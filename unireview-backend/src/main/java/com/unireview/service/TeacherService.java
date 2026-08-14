package com.unireview.service;

import com.unireview.dto.response.PagedResponse;
import com.unireview.dto.response.TeacherDetailResponse;
import com.unireview.dto.response.TeacherResponse;
import com.unireview.entity.Review;
import com.unireview.entity.Teacher;
import com.unireview.enums.ReviewStatus;
import com.unireview.exception.ResourceNotFoundException;
import com.unireview.repository.ReviewRepository;
import com.unireview.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public PagedResponse<TeacherResponse> getTeachers(
            String search,
            String faculty,
            BigDecimal minRating,
            String sortBy,
            String sortDir,
            int page,
            int size
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir != null ? sortDir : "DESC"),
                "rating".equalsIgnoreCase(sortBy) ? "avgRating" :
                "reviews".equalsIgnoreCase(sortBy) ? "totalReviews" : "fullName");

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Teacher> teacherPage = teacherRepository.findByFilters(search, faculty, minRating, pageable);

        List<TeacherResponse> content = teacherPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PagedResponse.<TeacherResponse>builder()
                .content(content)
                .page(teacherPage.getNumber())
                .size(teacherPage.getSize())
                .totalElements(teacherPage.getTotalElements())
                .totalPages(teacherPage.getTotalPages())
                .last(teacherPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public TeacherDetailResponse getTeacherDetail(Long id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giảng viên ID: " + id));

        List<Review> approvedReviews = reviewRepository.findByTeacherIdAndStatusOrderByCreatedAtDesc(id, ReviewStatus.APPROVED);

        // Rating distribution (1 to 5 stars)
        Map<Integer, Integer> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) distribution.put(i, 0);

        // Multiple choice stats
        Map<String, Map<String, Integer>> mcStats = new HashMap<>();
        mcStats.put("difficulty", new HashMap<>());
        mcStats.put("attendance", new HashMap<>());
        mcStats.put("materialsAllowed", new HashMap<>());
        mcStats.put("wouldRecommend", new HashMap<>());
        mcStats.put("workload", new HashMap<>());

        for (Review r : approvedReviews) {
            int stars = r.getRatingOverall();
            distribution.put(stars, distribution.getOrDefault(stars, 0) + 1);

            incrementCount(mcStats.get("difficulty"), r.getDifficulty().name());
            incrementCount(mcStats.get("attendance"), r.getAttendance().name());
            incrementCount(mcStats.get("materialsAllowed"), r.getMaterialsAllowed().name());
            incrementCount(mcStats.get("wouldRecommend"), r.getWouldRecommend().name());
            incrementCount(mcStats.get("workload"), r.getWorkload().name());
        }

        return TeacherDetailResponse.builder()
                .id(teacher.getId())
                .fullName(teacher.getFullName())
                .title(teacher.getTitle())
                .faculty(teacher.getFaculty())
                .department(teacher.getDepartment())
                .avatarUrl(teacher.getAvatarUrl())
                .avgRating(teacher.getAvgRating())
                .totalReviews(teacher.getTotalReviews())
                .ratingDistribution(distribution)
                .multipleChoiceStats(mcStats)
                .build();
    }

    private void incrementCount(Map<String, Integer> map, String key) {
        map.put(key, map.getOrDefault(key, 0) + 1);
    }

    private TeacherResponse mapToResponse(Teacher t) {
        return TeacherResponse.builder()
                .id(t.getId())
                .fullName(t.getFullName())
                .title(t.getTitle())
                .faculty(t.getFaculty())
                .department(t.getDepartment())
                .avatarUrl(t.getAvatarUrl())
                .avgRating(t.getAvgRating())
                .totalReviews(t.getTotalReviews())
                .build();
    }
}
