package com.unireview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherResponse {
    private Long id;
    private String fullName;
    private String title;
    private String faculty;
    private String department;
    private String avatarUrl;
    private BigDecimal avgRating;
    private Integer totalReviews;
}
