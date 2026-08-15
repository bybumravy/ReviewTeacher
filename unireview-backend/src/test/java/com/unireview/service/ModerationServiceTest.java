package com.unireview.service;

import com.unireview.enums.ReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ModerationServiceTest {

    private RestTemplate restTemplate;
    private ModerationService moderationService;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        moderationService = new ModerationService(new DefaultResourceLoader(), restTemplate);
        moderationService.initBannedWords();
        ReflectionTestUtils.setField(moderationService, "apiUrl", "https://commentanalyzer.googleapis.com/v1alpha1/comments:analyze");
        ReflectionTestUtils.setField(moderationService, "toxicityThreshold", 0.7);
    }

    @Test
    void containsBannedContent_detectsBannedWord() {
        assertTrue(moderationService.containsBannedContent("Thầy này dmm dạy chán quá"));
    }

    @Test
    void containsBannedContent_detectsPhoneNumber() {
        assertTrue(moderationService.containsBannedContent("Liên hệ mình qua số 0912345678 nhé"));
    }

    @Test
    void containsBannedContent_detectsEmail() {
        assertTrue(moderationService.containsBannedContent("Email cho tôi tại test@example.com"));
    }

    @Test
    void containsBannedContent_detectsUrl() {
        assertTrue(moderationService.containsBannedContent("Xem thêm tại https://spam-link.com"));
    }

    @Test
    void containsBannedContent_allowsCleanContent() {
        assertFalse(moderationService.containsBannedContent("Thầy dạy rất nhiệt tình và dễ hiểu"));
    }

    @Test
    void evaluateContent_bannedContent_isRejected() {
        ModerationService.ModerationResult result = moderationService.evaluateContent("Đồ dmm ngu quá");
        assertEquals(ReviewStatus.REJECTED, result.status());
    }

    @Test
    void evaluateContent_mockKeyBypass_isApprovedWithoutCallingApi() {
        // Default apiKey field value ("mock_perspective_key") triggers the fail-open bypass (T013 fix)
        ReflectionTestUtils.setField(moderationService, "apiKey", "mock_perspective_key");

        ModerationService.ModerationResult result = moderationService.evaluateContent("Thầy dạy rất hay và tận tâm");

        assertEquals(ReviewStatus.APPROVED, result.status());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void evaluateContent_realKeyLowToxicity_isApproved() {
        ReflectionTestUtils.setField(moderationService, "apiKey", "real_key_123");
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(perspectiveResponse(0.2));

        ModerationService.ModerationResult result = moderationService.evaluateContent("Thầy dạy rất hay");

        assertEquals(ReviewStatus.APPROVED, result.status());
    }

    @Test
    void evaluateContent_realKeyHighToxicity_isFlagged() {
        ReflectionTestUtils.setField(moderationService, "apiKey", "real_key_123");
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(perspectiveResponse(0.9));

        ModerationService.ModerationResult result = moderationService.evaluateContent("Nội dung nghi vấn");

        assertEquals(ReviewStatus.FLAGGED, result.status());
    }

    @Test
    void evaluateContent_apiThrows_failsOpenToApproved() {
        // Second, distinct fail-open path: the API call itself throws (not the mock-key bypass)
        ReflectionTestUtils.setField(moderationService, "apiKey", "real_key_123");
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenThrow(new RestClientException("Perspective API unavailable"));

        ModerationService.ModerationResult result = moderationService.evaluateContent("Nội dung hợp lệ");

        assertEquals(ReviewStatus.APPROVED, result.status());
    }

    private Map<String, Object> perspectiveResponse(double score) {
        Map<String, Object> summaryScore = Map.of("value", score);
        Map<String, Object> toxicity = Map.of("summaryScore", summaryScore);
        Map<String, Object> attributeScores = Map.of("TOXICITY", toxicity);
        return Map.of("attributeScores", attributeScores);
    }
}
