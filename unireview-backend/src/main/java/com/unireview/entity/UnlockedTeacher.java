package com.unireview.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "unlocked_teachers", uniqueConstraints = {
    @UniqueConstraint(name = "uq_reviewer_unlock", columnNames = {"reviewer_token", "teacher_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnlockedTeacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reviewer_token", nullable = false, length = 50)
    private String reviewerToken;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @CreationTimestamp
    @Column(name = "unlocked_at", updatable = false)
    private LocalDateTime unlockedAt;
}
