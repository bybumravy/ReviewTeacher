package com.unireview.entity;

import com.unireview.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews", uniqueConstraints = {
    @UniqueConstraint(name = "uq_reviewer_teacher", columnNames = {"reviewer_token", "teacher_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reviewer_token", nullable = false, length = 50)
    private String reviewerToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(name = "rating_overall", nullable = false)
    private Integer ratingOverall;

    @Column(name = "rating_teaching", nullable = false)
    private Integer ratingTeaching;

    @Column(name = "rating_grading", nullable = false)
    private Integer ratingGrading;

    @Column(name = "rating_personality", nullable = false)
    private Integer ratingPersonality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Attendance attendance;

    @Enumerated(EnumType.STRING)
    @Column(name = "materials_allowed", nullable = false, length = 20)
    private MaterialsAllowed materialsAllowed;

    @Enumerated(EnumType.STRING)
    @Column(name = "would_recommend", nullable = false, length = 20)
    private Recommendation wouldRecommend;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Workload workload;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false, length = 20)
    private String semester;

    @Column(name = "upvote_count")
    @Builder.Default
    private Integer upvoteCount = 0;

    @Column(name = "downvote_count")
    @Builder.Default
    private Integer downvoteCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.PENDING;

    @Column(name = "toxicity_score", precision = 3, scale = 2)
    private BigDecimal toxicityScore;

    @Column(name = "ip_hash", length = 64)
    private String ipHash;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
