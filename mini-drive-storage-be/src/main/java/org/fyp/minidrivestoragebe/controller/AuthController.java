package org.fyp.minidrivestoragebe.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fyp.minidrivestoragebe.dto.auth.AuthResponse;
import org.fyp.minidrivestoragebe.dto.auth.LoginRequest;
import org.fyp.minidrivestoragebe.dto.auth.RegisterRequest;
import org.fyp.minidrivestoragebe.dto.response.ApiResponse;
import org.fyp.minidrivestoragebe.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}
