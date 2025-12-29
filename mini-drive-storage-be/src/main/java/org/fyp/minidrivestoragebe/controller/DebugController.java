package org.fyp.minidrivestoragebe.controller;

import lombok.RequiredArgsConstructor;
import org.fyp.minidrivestoragebe.dto.response.ApiResponse;
import org.fyp.minidrivestoragebe.dto.response.GenerateSystemResponse;
import org.fyp.minidrivestoragebe.service.DebugService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/debug")
@RequiredArgsConstructor
public class DebugController {

    private final DebugService debugService;

    @PostMapping("/generate-system")
    public ResponseEntity<ApiResponse<GenerateSystemResponse>> generateSystem() {
        GenerateSystemResponse response = debugService.generateMockSystem();
        return ResponseEntity.ok(ApiResponse.success("Mock system generated successfully", response));
    }
}
