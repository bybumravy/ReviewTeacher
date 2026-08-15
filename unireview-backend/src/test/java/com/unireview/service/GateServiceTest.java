package com.unireview.service;

import com.unireview.entity.Reviewer;
import com.unireview.exception.InsufficientCreditException;
import com.unireview.exception.NoReviewerTokenException;
import com.unireview.repository.ReviewRepository;
import com.unireview.repository.ReviewerRepository;
import com.unireview.repository.UnlockedTeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GateServiceTest {

    private ReviewerRepository reviewerRepository;
    private UnlockedTeacherRepository unlockedTeacherRepository;
    private ReviewRepository reviewRepository;
    private GateService gateService;

    private static final String TOKEN = "rv_test_token";
    private static final Long TEACHER_ID = 1L;

    @BeforeEach
    void setUp() {
        reviewerRepository = mock(ReviewerRepository.class);
        unlockedTeacherRepository = mock(UnlockedTeacherRepository.class);
        reviewRepository = mock(ReviewRepository.class);
        gateService = new GateService(reviewerRepository, unlockedTeacherRepository, reviewRepository);
    }

    @Test
    void checkAndUnlockTeacher_noToken_throwsNoReviewerToken() {
        assertThrows(NoReviewerTokenException.class,
                () -> gateService.checkAndUnlockTeacher(null, TEACHER_ID));
        assertThrows(NoReviewerTokenException.class,
                () -> gateService.checkAndUnlockTeacher("  ", TEACHER_ID));
    }

    @Test
    void checkAndUnlockTeacher_alreadyUnlocked_shortCircuitsFree() {
        when(unlockedTeacherRepository.existsByReviewerTokenAndTeacherId(TOKEN, TEACHER_ID)).thenReturn(true);

        boolean result = gateService.checkAndUnlockTeacher(TOKEN, TEACHER_ID);

        assertTrue(result);
        verify(reviewerRepository, never()).findByToken(any());
        verify(unlockedTeacherRepository, never()).save(any());
    }

    @Test
    void checkAndUnlockTeacher_authoredReview_autoUnlocksFree() {
        when(unlockedTeacherRepository.existsByReviewerTokenAndTeacherId(TOKEN, TEACHER_ID)).thenReturn(false);
        when(reviewRepository.existsByReviewerTokenAndTeacherId(TOKEN, TEACHER_ID)).thenReturn(true);

        boolean result = gateService.checkAndUnlockTeacher(TOKEN, TEACHER_ID);

        assertTrue(result);
        verify(unlockedTeacherRepository).save(any());
        verify(reviewerRepository, never()).findByToken(any());
    }

    @Test
    void checkAndUnlockTeacher_sufficientCredit_deductsExactlyOneAndUnlocks() {
        when(unlockedTeacherRepository.existsByReviewerTokenAndTeacherId(TOKEN, TEACHER_ID)).thenReturn(false);
        when(reviewRepository.existsByReviewerTokenAndTeacherId(TOKEN, TEACHER_ID)).thenReturn(false);
        Reviewer reviewer = Reviewer.builder().id(1L).token(TOKEN).creditBalance(2).build();
        when(reviewerRepository.findByToken(TOKEN)).thenReturn(Optional.of(reviewer));

        boolean result = gateService.checkAndUnlockTeacher(TOKEN, TEACHER_ID);

        assertTrue(result);
        assertEquals(1, reviewer.getCreditBalance());
        verify(reviewerRepository).save(reviewer);
        verify(unlockedTeacherRepository).save(any());
    }

    @Test
    void checkAndUnlockTeacher_zeroCredit_throwsInsufficientCredit() {
        when(unlockedTeacherRepository.existsByReviewerTokenAndTeacherId(TOKEN, TEACHER_ID)).thenReturn(false);
        when(reviewRepository.existsByReviewerTokenAndTeacherId(TOKEN, TEACHER_ID)).thenReturn(false);
        Reviewer reviewer = Reviewer.builder().id(1L).token(TOKEN).creditBalance(0).build();
        when(reviewerRepository.findByToken(TOKEN)).thenReturn(Optional.of(reviewer));

        assertThrows(InsufficientCreditException.class,
                () -> gateService.checkAndUnlockTeacher(TOKEN, TEACHER_ID));

        verify(unlockedTeacherRepository, never()).save(any());
    }

    @Test
    void checkAndUnlockTeacher_unknownToken_throwsNoReviewerToken() {
        when(unlockedTeacherRepository.existsByReviewerTokenAndTeacherId(TOKEN, TEACHER_ID)).thenReturn(false);
        when(reviewRepository.existsByReviewerTokenAndTeacherId(TOKEN, TEACHER_ID)).thenReturn(false);
        when(reviewerRepository.findByToken(TOKEN)).thenReturn(Optional.empty());

        assertThrows(NoReviewerTokenException.class,
                () -> gateService.checkAndUnlockTeacher(TOKEN, TEACHER_ID));
    }

    @Test
    void checkAndUnlockTeacher_secondVisitAfterUnlock_spendsNoAdditionalCredit() {
        Reviewer reviewer = Reviewer.builder().id(1L).token(TOKEN).creditBalance(2).build();
        when(reviewerRepository.findByToken(TOKEN)).thenReturn(Optional.of(reviewer));
        when(unlockedTeacherRepository.existsByReviewerTokenAndTeacherId(TOKEN, TEACHER_ID))
                .thenReturn(false)
                .thenReturn(true); // simulate: unlocked after first call

        gateService.checkAndUnlockTeacher(TOKEN, TEACHER_ID);
        assertEquals(1, reviewer.getCreditBalance());

        gateService.checkAndUnlockTeacher(TOKEN, TEACHER_ID);
        assertEquals(1, reviewer.getCreditBalance()); // unchanged on second (already-unlocked) visit
    }
}
