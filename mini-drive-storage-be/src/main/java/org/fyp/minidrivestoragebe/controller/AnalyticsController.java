package org.fyp.minidrivestoragebe.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Analytics", description = "User storage statistics and analytics")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/usage")
    @Operation(summary = "Get storage usage", description = "Get current user's storage usage statistics including total used, quota, and file counts")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usage statistics retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<UsageStatsResponse>> getUserUsage(Authentication authentication) {
        String userEmail = authentication.getName();
        UsageStatsResponse stats = analyticsService.getUserUsageStats(userEmail);
        return ResponseEntity.ok(ApiResponse.success("Usage statistics retrieved successfully", stats));
    }
}
