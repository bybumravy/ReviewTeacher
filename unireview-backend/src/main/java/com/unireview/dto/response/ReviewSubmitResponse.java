package com.unireview.dto.response;

import com.unireview.enums.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSubmitResponse {
    private Long id;
    private ReviewStatus status;
    private String message;
    private String reviewerToken;
    private Integer creditBalance;
    private Long autoUnlockedTeacherId;
}
