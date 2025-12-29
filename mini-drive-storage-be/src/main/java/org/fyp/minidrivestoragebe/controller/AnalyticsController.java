package org.fyp.minidrivestoragebe.controller;

import lombok.RequiredArgsConstructor;
import org.fyp.minidrivestoragebe.dto.response.ApiResponse;
import org.fyp.minidrivestoragebe.dto.response.UsageStatsResponse;
import org.fyp.minidrivestoragebe.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/usage")
    public ResponseEntity<ApiResponse<UsageStatsResponse>> getUserUsage(Authentication authentication) {
        String userEmail = authentication.getName();
        UsageStatsResponse stats = analyticsService.getUserUsageStats(userEmail);
        return ResponseEntity.ok(ApiResponse.success("Usage statistics retrieved successfully", stats));
    }
}
