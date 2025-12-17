package org.fyp.minidrivestoragebe.service;

import lombok.RequiredArgsConstructor;
import org.fyp.minidrivestoragebe.config.JwtUtil;
import org.fyp.minidrivestoragebe.dto.auth.AuthResponse;
import org.fyp.minidrivestoragebe.dto.auth.LoginRequest;
import org.fyp.minidrivestoragebe.dto.auth.RegisterRequest;
import org.fyp.minidrivestoragebe.entity.User;
import org.fyp.minidrivestoragebe.exception.BadRequestException;
import org.fyp.minidrivestoragebe.exception.UnauthorizedException;
import org.fyp.minidrivestoragebe.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw BadRequestException.emailAlreadyExists();
        }

        // Create new user
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .storageUsed(0L)
                .storageQuota(10737418240L) // 10GB
                .enabled(true)
                .build();

        user = userRepository.save(user);

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail(), null);

        return AuthResponse.of(
                token,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getStorageUsed(),
                user.getStorageQuota()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            // Get user details
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(UnauthorizedException::invalidCredentials);

            // Generate JWT token
            String token = jwtUtil.generateToken(user.getEmail(), null);

            return AuthResponse.of(
                    token,
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getStorageUsed(),
                    user.getStorageQuota()
            );
        } catch (BadCredentialsException e) {
            throw UnauthorizedException.invalidCredentials();
        }
    }
}
