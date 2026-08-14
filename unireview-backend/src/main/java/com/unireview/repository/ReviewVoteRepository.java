package com.unireview.repository;

import com.unireview.entity.ReviewVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewVoteRepository extends JpaRepository<ReviewVote, Long> {
    Optional<ReviewVote> findByVoterTokenAndReviewId(String voterToken, Long reviewId);
    boolean existsByVoterTokenAndReviewId(String voterToken, Long reviewId);
}
