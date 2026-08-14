package com.unireview.dto.request;

import com.unireview.enums.*;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewCreateRequest {

    @NotNull(message = "ID giảng viên không được để trống")
    private Long teacherId;

    private Long subjectId;

    @NotNull
    @Min(1) @Max(5)
    private Integer ratingOverall;

    @NotNull
    @Min(1) @Max(5)
    private Integer ratingTeaching;

    @NotNull
    @Min(1) @Max(5)
    private Integer ratingGrading;

    @NotNull
    @Min(1) @Max(5)
    private Integer ratingPersonality;

    @NotNull
    private Difficulty difficulty;

    @NotNull
    private Attendance attendance;

    @NotNull
    private MaterialsAllowed materialsAllowed;

    @NotNull
    private Recommendation wouldRecommend;

    @NotNull
    private Workload workload;

    @NotBlank(message = "Nội dung nhận xét không được để trống")
    @Size(min = 50, message = "Nội dung nhận xét phải có ít nhất 50 ký tự")
    private String content;

    @NotBlank(message = "Học kỳ không được để trống")
    private String semester;

    private String captchaToken;
}
