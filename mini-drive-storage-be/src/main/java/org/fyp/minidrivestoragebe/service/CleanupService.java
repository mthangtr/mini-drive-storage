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
        
        int batchSize = 100;
        List<Future<Integer>> futures = new ArrayList<>();
        
        for (int i = 0; i < filesToDelete.size(); i += batchSize) {
            int end = Math.min(i + batchSize, filesToDelete.size());
            List<FileItem> batch = filesToDelete.subList(i, end);
            
            Future<Integer> future = executorService.submit(() -> processBatch(batch));
            futures.add(future);
        }
        
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

    private Integer processBatch(List<FileItem> batch) {
        int deletedCount = 0;
        
        for (FileItem fileItem : batch) {
            try {
                if (fileItem.getStoragePath() != null && !fileItem.getStoragePath().isEmpty()) {
                    deletePhysicalFile(fileItem.getStoragePath());
                }
                
                fileItemRepository.delete(fileItem);
                deletedCount++;
                
                log.debug("Permanently deleted file: {} (ID: {})", fileItem.getName(), fileItem.getId());
            } catch (Exception e) {
                log.error("Failed to delete file: {} (ID: {})", fileItem.getName(), fileItem.getId(), e);
            }
        }
        
        return deletedCount;
    }

    private void deletePhysicalFile(String storagePath) {
        try {
            Path path = Paths.get(storagePath);
            if (Files.exists(path)) {
                Files.delete(path);
                log.debug("Deleted physical file: {}", storagePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete physical file: {}", storagePath, e);
        }
    }
}
