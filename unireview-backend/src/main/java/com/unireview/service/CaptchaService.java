package com.unireview.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class CaptchaService {

    private final RestTemplate restTemplate;

    @Value("${app.captcha.secret-key:mock_secret}")
    private String secretKey;

    @Value("${app.captcha.verify-url:https://www.google.com/recaptcha/api/siteverify}")
    private String verifyUrl;

    @Value("${app.captcha.enabled:false}")
    private boolean captchaEnabled;

    public boolean verify(String captchaToken) {
        if (!captchaEnabled || "mock_secret".equals(secretKey)) {
            return true; // Bypass in dev/test mode
        }

        if (captchaToken == null || captchaToken.isBlank()) {
            return false;
        }

        try {
            String url = String.format("%s?secret=%s&response=%s", verifyUrl, secretKey, captchaToken);
            Map response = restTemplate.postForObject(url, null, Map.class);
            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                Double score = (Double) response.get("score");
                return score == null || score >= 0.5;
            }
        } catch (Exception e) {
            log.warn("reCAPTCHA verification error: {}", e.getMessage());
        }

        return false;
    }
}
