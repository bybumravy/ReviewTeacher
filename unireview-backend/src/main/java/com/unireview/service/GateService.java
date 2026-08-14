package com.unireview.service;

import com.unireview.dto.response.GateStatusResponse;
import com.unireview.entity.Reviewer;
import com.unireview.entity.UnlockedTeacher;
import com.unireview.enums.ReviewStatus;
import com.unireview.exception.InsufficientCreditException;
import com.unireview.exception.NoReviewerTokenException;
import com.unireview.repository.ReviewRepository;
import com.unireview.repository.ReviewerRepository;
import com.unireview.repository.UnlockedTeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GateService {

    private final ReviewerRepository reviewerRepository;
    private final UnlockedTeacherRepository unlockedTeacherRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public Reviewer getOrCreateReviewer(String token, String ipHash) {
        if (token == null || token.isBlank()) {
            token = UUID.randomUUID().toString();
        }
        final String finalToken = token;
        return reviewerRepository.findByToken(finalToken)
                .orElseGet(() -> {
                    Reviewer r = Reviewer.builder()
                            .token(finalToken)
                            .creditBalance(0)
                            .reviewCount(0)
                            .ipHash(ipHash)
                            .build();
                    return reviewerRepository.save(r);
                });
    }

    @Transactional
    public boolean checkAndUnlockTeacher(String reviewerToken, Long teacherId) {
        if (reviewerToken == null || reviewerToken.isBlank()) {
            throw new NoReviewerTokenException("Chưa có token định danh người dùng");
        }

        // 1. Already unlocked previously -> Free
        if (unlockedTeacherRepository.existsByReviewerTokenAndTeacherId(reviewerToken, teacherId)) {
            return true;
        }

        // 2. Authored a review for this teacher -> Auto unlock free
        if (reviewRepository.existsByReviewerTokenAndTeacherId(reviewerToken, teacherId)) {
            saveUnlockRecord(reviewerToken, teacherId);
            return true;
        }

        // 3. Check credits
        Reviewer reviewer = reviewerRepository.findByToken(reviewerToken)
                .orElseThrow(() -> new NoReviewerTokenException("Token không tồn tại trên hệ thống"));

        if (reviewer.getCreditBalance() <= 0) {
            throw new InsufficientCreditException("Bạn không đủ credit để xem. Cần viết thêm review!");
        }

        // 4. Deduct 1 credit & unlock
        reviewer.setCreditBalance(reviewer.getCreditBalance() - 1);
        reviewerRepository.save(reviewer);
        saveUnlockRecord(reviewerToken, teacherId);

        return true;
    }

    @Transactional
    public void addCreditAndUnlock(String reviewerToken, Long teacherId) {
        Reviewer reviewer = reviewerRepository.findByToken(reviewerToken)
                .orElseGet(() -> reviewerRepository.save(Reviewer.builder().token(reviewerToken).build()));

        reviewer.setCreditBalance(reviewer.getCreditBalance() + 1);
        reviewer.setReviewCount(reviewer.getReviewCount() + 1);
        reviewerRepository.save(reviewer);

        saveUnlockRecord(reviewerToken, teacherId);
    }

    @Transactional(readOnly = true)
    public GateStatusResponse getGateStatus(String reviewerToken) {
        if (reviewerToken == null || reviewerToken.isBlank()) {
            return new GateStatusResponse(0, 0, 0, List.of());
        }

        Reviewer reviewer = reviewerRepository.findByToken(reviewerToken).orElse(null);
        if (reviewer == null) {
            return new GateStatusResponse(0, 0, 0, List.of());
        }

        List<Long> unlockedIds = unlockedTeacherRepository.findByReviewerToken(reviewerToken)
                .stream()
                .map(UnlockedTeacher::getTeacherId)
                .toList();

        int pendingCount = reviewRepository.countByReviewerTokenAndStatus(reviewerToken, ReviewStatus.PENDING);

        return GateStatusResponse.builder()
                .creditBalance(reviewer.getCreditBalance())
                .pendingReviews(pendingCount)
                .totalReviews(reviewer.getReviewCount())
                .unlockedTeacherIds(unlockedIds)
                .build();
    }

    private void saveUnlockRecord(String reviewerToken, Long teacherId) {
        if (!unlockedTeacherRepository.existsByReviewerTokenAndTeacherId(reviewerToken, teacherId)) {
            UnlockedTeacher unlock = UnlockedTeacher.builder()
                    .reviewerToken(reviewerToken)
                    .teacherId(teacherId)
                    .build();
            unlockedTeacherRepository.save(unlock);
        }
    }
}
