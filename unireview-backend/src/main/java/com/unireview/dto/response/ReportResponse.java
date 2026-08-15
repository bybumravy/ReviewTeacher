package com.unireview.dto.response;

import com.unireview.enums.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private Long id;
    private Long reviewId;
    private String reviewContent;
    private String reason;
    private String description;
    private ReportStatus status;
    private LocalDateTime createdAt;
}
