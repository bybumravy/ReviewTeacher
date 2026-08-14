package com.unireview.controller;

import com.unireview.dto.request.AdminLoginRequest;
import com.unireview.dto.response.PagedResponse;
import com.unireview.dto.response.ReviewResponse;
import com.unireview.service.AdminService;
import com.unireview.service.CsvImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin API", description = "Admin authentication, review moderation, and teacher CSV imports")
public class AdminController {

    private final AdminService adminService;
    private final CsvImportService csvImportService;

    @PostMapping("/login")
    @Operation(summary = "Admin login to receive JWT token")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody AdminLoginRequest request) {
        return ResponseEntity.ok(adminService.login(request));
    }

    @GetMapping("/reviews/flagged")
    @Operation(summary = "Get list of reviews flagged by AI for manual admin review")
    public ResponseEntity<PagedResponse<ReviewResponse>> getFlaggedReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(adminService.getFlaggedReviews(page, size));
    }

    @PutMapping("/reviews/{id}/approve")
    @Operation(summary = "Approve a flagged review -> Grants credit to reviewer & auto-unlocks teacher")
    public ResponseEntity<Void> approveReview(@PathVariable Long id) {
        adminService.approveReview(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/reviews/{id}/reject")
    @Operation(summary = "Reject a flagged review")
    public ResponseEntity<Void> rejectReview(@PathVariable Long id) {
        adminService.rejectReview(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/teachers/import-csv")
    @Operation(summary = "Import teachers list from a CSV file")
    public ResponseEntity<Map<String, Object>> importTeachersCsv(@RequestParam("file") MultipartFile file) throws Exception {
        int count = csvImportService.importTeachersFromCsv(file);
        return ResponseEntity.ok(Map.of("message", "Đã import thành công " + count + " giảng viên", "importedCount", count));
    }
}
