package org.fyp.minidrivestoragebe.service;

import org.fyp.minidrivestoragebe.dto.response.UsageStatsResponse;
import org.fyp.minidrivestoragebe.entity.User;
import org.fyp.minidrivestoragebe.enums.FileType;
import org.fyp.minidrivestoragebe.exception.ResourceNotFoundException;
import org.fyp.minidrivestoragebe.repository.FileItemRepository;
import org.fyp.minidrivestoragebe.repository.FilePermissionRepository;
import org.fyp.minidrivestoragebe.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService Unit Tests")
class AnalyticsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileItemRepository fileItemRepository;

    @Mock
    private FilePermissionRepository filePermissionRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user-1")
                .email("test@example.com")
                .fullName("Test User")
                .storageUsed(5368709120L) // 5GB
                .storageQuota(10737418240L) // 10GB
                .build();
    }

    @Test
    @DisplayName("Should get usage stats successfully")
    void shouldGetUsageStatsSuccessfully() {
        // Given
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(fileItemRepository.countByOwnerAndTypeAndDeleted(testUser, FileType.FILE, false)).thenReturn(100L);
        when(fileItemRepository.countByOwnerAndTypeAndDeleted(testUser, FileType.FOLDER, false)).thenReturn(20L);
        when(filePermissionRepository.countByUser(testUser)).thenReturn(15L);
        when(filePermissionRepository.countByFileItemOwnerAndUserNot(testUser, testUser)).thenReturn(25L);

        // When
        UsageStatsResponse response = analyticsService.getUserUsageStats(testUser.getEmail());

        // Then
        assertNotNull(response);
        assertEquals(5368709120L, response.getStorageUsed());
        assertEquals(10737418240L, response.getStorageQuota());
        assertEquals(5368709120L, response.getStorageAvailable());
        assertEquals(50.0, response.getUsagePercentage());
        assertEquals(100L, response.getTotalFiles());
        assertEquals(20L, response.getTotalFolders());
        assertEquals(15L, response.getTotalSharedWithMe());
        assertEquals(25L, response.getTotalSharedByMe());
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                analyticsService.getUserUsageStats("nonexistent@example.com"));
    }

    @Test
    @DisplayName("Should calculate usage percentage correctly when storage quota is zero")
    void shouldCalculateUsagePercentageCorrectlyWhenQuotaIsZero() {
        // Given
        testUser.setStorageQuota(0L);
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(fileItemRepository.countByOwnerAndTypeAndDeleted(any(), any(), eq(false))).thenReturn(0L);
        when(filePermissionRepository.countByUser(any())).thenReturn(0L);
        when(filePermissionRepository.countByFileItemOwnerAndUserNot(any(), any())).thenReturn(0L);

        // When
        UsageStatsResponse response = analyticsService.getUserUsageStats(testUser.getEmail());

        // Then
        assertEquals(0.0, response.getUsagePercentage());
    }

    @Test
    @DisplayName("Should handle zero storage usage")
    void shouldHandleZeroStorageUsage() {
        // Given
        testUser.setStorageUsed(0L);
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(fileItemRepository.countByOwnerAndTypeAndDeleted(any(), any(), eq(false))).thenReturn(0L);
        when(filePermissionRepository.countByUser(any())).thenReturn(0L);
        when(filePermissionRepository.countByFileItemOwnerAndUserNot(any(), any())).thenReturn(0L);

        // When
        UsageStatsResponse response = analyticsService.getUserUsageStats(testUser.getEmail());

        // Then
        assertEquals(0L, response.getStorageUsed());
        assertEquals(testUser.getStorageQuota(), response.getStorageAvailable());
        assertEquals(0.0, response.getUsagePercentage());
    }
}
