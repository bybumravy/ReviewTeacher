package com.unireview.dto.response;

import com.unireview.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long teacherId;
    private String teacherName;
    private Integer ratingOverall;
    private Integer ratingTeaching;
    private Integer ratingGrading;
    private Integer ratingPersonality;
    private Difficulty difficulty;
    private Attendance attendance;
    private MaterialsAllowed materialsAllowed;
    private Recommendation wouldRecommend;
    private Workload workload;
    private String content;
    private String semester;
    private Integer upvoteCount;
    private Integer downvoteCount;
    private ReviewStatus status;
    private LocalDateTime createdAt;
}
