package com.unireview.controller;

import com.unireview.dto.response.GateStatusResponse;
import com.unireview.service.GateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gate")
@RequiredArgsConstructor
@Tag(name = "Gate API", description = "Check current credit balance and unlocked teacher IDs")
public class GateController {

    private final GateService gateService;

    @GetMapping("/status")
    @Operation(summary = "Get current reviewer credit status, pending count, and unlocked teacher IDs")
    public ResponseEntity<GateStatusResponse> getGateStatus(
            @RequestHeader(value = "X-Reviewer-Token", required = false) String reviewerToken,
            @CookieValue(value = "reviewer_token", required = false) String cookieToken
    ) {
        String token = (reviewerToken != null && !reviewerToken.isBlank()) ? reviewerToken : cookieToken;
        GateStatusResponse status = gateService.getGateStatus(token);
        return ResponseEntity.ok(status);
    }
}
