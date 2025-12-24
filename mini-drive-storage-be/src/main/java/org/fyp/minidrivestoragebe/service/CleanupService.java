package org.fyp.minidrivestoragebe.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.minidrivestoragebe.entity.FileItem;
import org.fyp.minidrivestoragebe.repository.FileItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CleanupService {

    private final FileItemRepository fileItemRepository;
    private final FileStorageService fileStorageService;
    private final ExecutorService executorService;

    @Value("${app.cleanup.retention-days:30}")
    private int retentionDays;

    /**
     * Scheduled cleanup task - runs at 2:00 AM every day
     * Cron format: second minute hour day month day-of-week
     */
    @Scheduled(cron = "${app.cleanup.cron:0 0 2 * * ?}")
    @Transactional
    public void cleanupDeletedFiles() {
        log.info("Starting scheduled cleanup of deleted files older than {} days", retentionDays);
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        List<FileItem> filesToDelete = fileItemRepository.findByDeletedTrueAndDeletedAtBefore(cutoffDate);
        
        if (filesToDelete.isEmpty()) {
            log.info("No files to cleanup");
            return;
        }
        
        log.info("Found {} files to permanently delete", filesToDelete.size());
        
        // Use multi-threading to process deletions
        int batchSize = 100;
        List<Future<Integer>> futures = new ArrayList<>();
        
        for (int i = 0; i < filesToDelete.size(); i += batchSize) {
            int end = Math.min(i + batchSize, filesToDelete.size());
            List<FileItem> batch = filesToDelete.subList(i, end);
            
            Future<Integer> future = executorService.submit(() -> processBatch(batch));
            futures.add(future);
        }
        
        // Wait for all batches to complete
        int totalDeleted = 0;
        int totalFailed = 0;
        
        for (Future<Integer> future : futures) {
            try {
                int deleted = future.get(5, TimeUnit.MINUTES);
                totalDeleted += deleted;
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.error("Error processing batch", e);
                totalFailed++;
            }
        }
        
        log.info("Cleanup completed: {} files deleted, {} batches failed", totalDeleted, totalFailed);
    }

    /**
     * Process a batch of files for deletion
     */
    private Integer processBatch(List<FileItem> batch) {
        int deleted = 0;
        
        for (FileItem fileItem : batch) {
            try {
                // Delete physical file if it exists
                if (fileItem.getStoragePath() != null && !fileItem.getStoragePath().isEmpty()) {
                    deletePhysicalFile(fileItem.getStoragePath());
                }
                
                // Delete from database
                fileItemRepository.delete(fileItem);
                deleted++;
                
                log.debug("Permanently deleted file: {} (ID: {})", fileItem.getName(), fileItem.getId());
            } catch (Exception e) {
                log.error("Failed to delete file: {} (ID: {})", fileItem.getName(), fileItem.getId(), e);
            }
        }
        
        return deleted;
    }

    /**
     * Delete physical file from storage
     */
    private void deletePhysicalFile(String storagePath) {
        try {
            Path path = Paths.get(storagePath);
            if (Files.exists(path)) {
                Files.delete(path);
                log.debug("Deleted physical file: {}", storagePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete physical file: {}", storagePath, e);
            // Don't throw exception, continue with database deletion
        }
    }

    /**
     * Manual trigger for cleanup (for testing or manual operations)
     */
    @Transactional
    public int manualCleanup(int daysOld) {
        log.info("Manual cleanup triggered for files older than {} days", daysOld);
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        List<FileItem> filesToDelete = fileItemRepository.findByDeletedTrueAndDeletedAtBefore(cutoffDate);
        
        int deleted = 0;
        for (FileItem fileItem : filesToDelete) {
            try {
                if (fileItem.getStoragePath() != null && !fileItem.getStoragePath().isEmpty()) {
                    deletePhysicalFile(fileItem.getStoragePath());
                }
                fileItemRepository.delete(fileItem);
                deleted++;
            } catch (Exception e) {
                log.error("Failed to delete file during manual cleanup: {}", fileItem.getId(), e);
            }
        }
        
        log.info("Manual cleanup completed: {} files deleted", deleted);
        return deleted;
    }
}
