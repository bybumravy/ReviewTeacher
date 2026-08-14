package com.unireview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDetailResponse {
    private Long id;
    private String fullName;
    private String title;
    private String faculty;
    private String department;
    private String avatarUrl;
    private BigDecimal avgRating;
    private Integer totalReviews;
    private Map<Integer, Integer> ratingDistribution;
    private Map<String, Map<String, Integer>> multipleChoiceStats;
}
