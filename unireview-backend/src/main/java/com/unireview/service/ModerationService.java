package com.unireview.service;

import com.unireview.enums.ReviewStatus;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class ModerationService {

    private final ResourceLoader resourceLoader;
    private final RestTemplate restTemplate;

    @Value("${app.moderation.perspective-api-key:mock_key}")
    private String apiKey;

    @Value("${app.moderation.perspective-api-url:https://commentanalyzer.googleapis.com/v1alpha1/comments:analyze}")
    private String apiUrl;

    @Value("${app.moderation.toxicity-threshold:0.7}")
    private double toxicityThreshold;

    private final Set<String> bannedWords = new HashSet<>();

    // Patterns for Phone, Email, URLs
    private static final Pattern PHONE_PATTERN = Pattern.compile(".*\\b0[0-9]{9}\\b.*");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(".*\\b[\\w.-]+@[\\w.-]+\\.[a-z]{2,}\\b.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL_PATTERN = Pattern.compile(".*https?://\\S+.*", Pattern.CASE_INSENSITIVE);

    @PostConstruct
    public void initBannedWords() {
        try {
            Resource resource = resourceLoader.getResource("classpath:banned_words.txt");
            if (resource.exists()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.trim().isEmpty()) {
                            bannedWords.add(line.trim().toLowerCase());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not load banned_words.txt", e);
        }
    }

    public boolean containsBannedContent(String content) {
        if (content == null || content.isBlank()) return false;
        String lower = content.toLowerCase();

        // 1. Check Phone numbers
        if (PHONE_PATTERN.matcher(lower).matches()) return true;

        // 2. Check Email addresses
        if (EMAIL_PATTERN.matcher(lower).matches()) return true;

        // 3. Check Website links
        if (URL_PATTERN.matcher(lower).matches()) return true;

        // 4. Check Banned words dictionary
        for (String word : bannedWords) {
            if (lower.contains(word)) return true;
        }

        return false;
    }

    public ModerationResult evaluateContent(String content) {
        // Layer 1: Local Regex & Banned words
        if (containsBannedContent(content)) {
            return new ModerationResult(ReviewStatus.REJECTED, BigDecimal.ONE);
        }

        // Layer 2: Google Perspective API (Mock or Live)
        try {
            if ("mock_key".equals(apiKey) || apiKey.isBlank()) {
                // If API key not set, default auto-approve (fail-open)
                return new ModerationResult(ReviewStatus.APPROVED, BigDecimal.valueOf(0.1));
            }

            double score = callPerspectiveApi(content);
            ReviewStatus status = (score < toxicityThreshold) ? ReviewStatus.APPROVED : ReviewStatus.FLAGGED;
            return new ModerationResult(status, BigDecimal.valueOf(score));
        } catch (Exception e) {
            log.warn("Perspective API call failed, fail-open to APPROVED: {}", e.getMessage());
            return new ModerationResult(ReviewStatus.APPROVED, BigDecimal.valueOf(0.15));
        }
    }

    private double callPerspectiveApi(String content) {
        String url = apiUrl + "?key=" + apiKey;

        Map<String, Object> comment = Map.of("text", content);
        Map<String, Object> requestedAttributes = Map.of("TOXICITY", Map.of());
        Map<String, Object> requestBody = Map.of(
                "comment", comment,
                "languages", List.of("vi", "en"),
                "requestedAttributes", requestedAttributes
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        Map response = restTemplate.postForObject(url, entity, Map.class);

        if (response != null && response.containsKey("attributeScores")) {
            Map attributeScores = (Map) response.get("attributeScores");
            Map toxicity = (Map) attributeScores.get("TOXICITY");
            Map summaryScore = (Map) toxicity.get("summaryScore");
            return ((Number) summaryScore.get("value")).doubleValue();
        }

        return 0.1;
    }

    public record ModerationResult(ReviewStatus status, BigDecimal toxicityScore) {}
}
