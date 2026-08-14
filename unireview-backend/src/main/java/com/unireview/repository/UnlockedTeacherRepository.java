package com.unireview.repository;

import com.unireview.entity.UnlockedTeacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnlockedTeacherRepository extends JpaRepository<UnlockedTeacher, Long> {
    boolean existsByReviewerTokenAndTeacherId(String reviewerToken, Long teacherId);
    List<UnlockedTeacher> findByReviewerToken(String reviewerToken);
}
