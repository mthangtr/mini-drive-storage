package org.fyp.minidrivestoragebe.service;

import org.fyp.minidrivestoragebe.dto.auth.AuthResponse;
import org.fyp.minidrivestoragebe.dto.auth.LoginRequest;
import org.fyp.minidrivestoragebe.dto.auth.RegisterRequest;
import org.fyp.minidrivestoragebe.entity.User;
import org.fyp.minidrivestoragebe.exception.BadRequestException;
import org.fyp.minidrivestoragebe.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private org.fyp.minidrivestoragebe.config.JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user-1")
                .email("test@example.com")
                .password("encodedPassword")
                .fullName("Test User")
                .storageUsed(0L)
                .storageQuota(10737418240L)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("Should register user successfully")
    void shouldRegisterUserSuccessfully() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@example.com")
                .password("password123")
                .fullName("New User")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User user = i.getArgument(0);
            user.setId("new-user-id");
            return user;
        });
        when(jwtUtil.generateToken(any())).thenReturn("jwt-token");

        // When
        AuthResponse response = authService.register(request);

        // Then
        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("newuser@example.com", response.getEmail());
        assertEquals("New User", response.getFullName());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .email("existing@example.com")
                .password("password123")
                .fullName("Existing User")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // When & Then
        assertThrows(BadRequestException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should login successfully")
    void shouldLoginSuccessfully() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(any())).thenReturn("jwt-token");

        // When
        AuthResponse response = authService.login(request);

        // Then
        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getFullName());
    }

    @Test
    @DisplayName("Should throw exception when credentials are invalid")
    void shouldThrowExceptionWhenCredentialsAreInvalid() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("wrongpassword")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When & Then
        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(userRepository, never()).findByEmail(any());
    }
}
