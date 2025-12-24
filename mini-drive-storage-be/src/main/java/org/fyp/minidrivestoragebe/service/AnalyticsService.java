package org.fyp.minidrivestoragebe.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.minidrivestoragebe.dto.response.UsageStatsResponse;
import org.fyp.minidrivestoragebe.entity.User;
import org.fyp.minidrivestoragebe.enums.FileType;
import org.fyp.minidrivestoragebe.exception.ResourceNotFoundException;
import org.fyp.minidrivestoragebe.repository.FileItemRepository;
import org.fyp.minidrivestoragebe.repository.FilePermissionRepository;
import org.fyp.minidrivestoragebe.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final UserRepository userRepository;
    private final FileItemRepository fileItemRepository;
    private final FilePermissionRepository filePermissionRepository;

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public UsageStatsResponse getUserUsageStats(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Long storageUsed = user.getStorageUsed();
        Long storageQuota = user.getStorageQuota();
        Long storageAvailable = storageQuota - storageUsed;
        Double usagePercentage = storageQuota > 0 ? (storageUsed * 100.0) / storageQuota : 0.0;

        // Count files owned by user (not deleted)
        Long totalFiles = fileItemRepository.countByOwnerAndTypeAndDeleted(user, FileType.FILE, false);
        
        // Count folders owned by user (not deleted)
        Long totalFolders = fileItemRepository.countByOwnerAndTypeAndDeleted(user, FileType.FOLDER, false);
        
        // Count items shared with me
        Long totalSharedWithMe = filePermissionRepository.countByUser(user);
        
        // Count items I shared with others
        Long totalSharedByMe = filePermissionRepository.countByFileItemOwnerAndUserNot(user, user);

        log.info("Usage stats retrieved for user: {}", userEmail);

        return UsageStatsResponse.builder()
                .storageUsed(storageUsed)
                .storageQuota(storageQuota)
                .storageAvailable(storageAvailable)
                .usagePercentage(Math.round(usagePercentage * 100.0) / 100.0)
                .totalFiles(totalFiles)
                .totalFolders(totalFolders)
                .totalSharedWithMe(totalSharedWithMe)
                .totalSharedByMe(totalSharedByMe)
                .build();
    }
}
