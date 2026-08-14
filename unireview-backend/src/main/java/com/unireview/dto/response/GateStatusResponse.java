package com.unireview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GateStatusResponse {
    private Integer creditBalance;
    private Integer pendingReviews;
    private Integer totalReviews;
    private List<Long> unlockedTeacherIds;
}
