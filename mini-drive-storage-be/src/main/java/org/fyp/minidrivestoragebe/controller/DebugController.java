package org.fyp.minidrivestoragebe.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.fyp.minidrivestoragebe.dto.response.ApiResponse;
import org.fyp.minidrivestoragebe.dto.response.GenerateSystemResponse;
import org.fyp.minidrivestoragebe.service.DebugService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/debug")
@RequiredArgsConstructor
@Tag(name = "Debug", description = "Development/testing endpoints - should be secured in production")
public class DebugController {

    private final DebugService debugService;

    @PostMapping("/generate-system")
    @Operation(
            summary = "Generate mock system data",
            description = "Creates 10 users, ~10,000 files total, with 10% random sharing between users. WARNING: This will delete existing test data!"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mock system generated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<GenerateSystemResponse>> generateSystem() {
        GenerateSystemResponse response = debugService.generateMockSystem();
        return ResponseEntity.ok(ApiResponse.success("Mock system generated successfully", response));
    }
}
