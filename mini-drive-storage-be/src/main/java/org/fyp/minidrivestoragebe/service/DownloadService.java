package org.fyp.minidrivestoragebe.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.minidrivestoragebe.dto.file.DownloadStatusResponse;
import org.fyp.minidrivestoragebe.entity.DownloadRequest;
import org.fyp.minidrivestoragebe.entity.FileItem;
import org.fyp.minidrivestoragebe.entity.User;
import org.fyp.minidrivestoragebe.enums.DownloadStatus;
import org.fyp.minidrivestoragebe.enums.FileType;
import org.fyp.minidrivestoragebe.exception.BadRequestException;
import org.fyp.minidrivestoragebe.exception.ResourceNotFoundException;
import org.fyp.minidrivestoragebe.repository.DownloadRequestRepository;
import org.fyp.minidrivestoragebe.repository.FileItemRepository;
import org.fyp.minidrivestoragebe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class DownloadService {

    private final DownloadRequestRepository downloadRequestRepository;
    private final FileItemRepository fileItemRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Value("${app.storage.location}")
    private String storageLocation;

    /**
     * Initiate folder download (async zip creation)
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public DownloadStatusResponse initiateFolderDownload(String folderId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        FileItem folder = fileItemRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));

        if (folder.getType() != FileType.FOLDER) {
            throw new BadRequestException("Only folders can be downloaded asynchronously");
        }

        // Create download request
        String requestId = UUID.randomUUID().toString();
        DownloadRequest downloadRequest = DownloadRequest.builder()
                .requestId(requestId)
                .fileItem(folder)
                .user(user)
                .status(DownloadStatus.PENDING)
                .build();

        downloadRequest = downloadRequestRepository.save(downloadRequest);

        // Trigger async zip creation
        processDownloadRequestAsync(downloadRequest.getId());

        return DownloadStatusResponse.of(
                requestId,
                DownloadStatus.PENDING,
                null,
                "Folder download initiated. Use the requestId to check status."
        );
    }

    /**
     * Get download status
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public DownloadStatusResponse getDownloadStatus(String requestId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        DownloadRequest request = downloadRequestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Download request not found"));

        // Check ownership
        if (!request.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to access this download request");
        }

        String downloadUrl = null;
        if (request.getStatus() == DownloadStatus.READY && request.getDownloadPath() != null) {
            downloadUrl = "/api/v1/files/downloads/" + requestId + "/file";
        }

        String message = switch (request.getStatus()) {
            case PENDING -> "Download request is pending";
            case PROCESSING -> "Zip file is being created";
            case READY -> "Zip file is ready for download";
            case FAILED -> "Download failed: " + request.getErrorMessage();
        };

        return DownloadStatusResponse.of(
                requestId,
                request.getStatus(),
                downloadUrl,
                message
        );
    }

    /**
     * Async processing of download request (zip creation)
     */
    @Async("taskExecutor")
    public void processDownloadRequestAsync(String downloadRequestId) {
        try {
            DownloadRequest request = downloadRequestRepository.findById(downloadRequestId)
                    .orElseThrow(() -> new ResourceNotFoundException("Download request not found"));

            log.info("Starting async zip creation for request: {}", request.getRequestId());

            // Update status to PROCESSING
            request.setStatus(DownloadStatus.PROCESSING);
            downloadRequestRepository.save(request);

            // Create zip file
            String zipPath = createZipFile(request.getFileItem(), request.getUser().getId());

            // Update status to READY
            request.setStatus(DownloadStatus.READY);
            request.setDownloadPath(zipPath);
            downloadRequestRepository.save(request);

            log.info("Zip creation completed for request: {}", request.getRequestId());

        } catch (Exception e) {
            log.error("Failed to create zip file for request: {}", downloadRequestId, e);

            // Update status to FAILED
            downloadRequestRepository.findById(downloadRequestId).ifPresent(req -> {
                req.setStatus(DownloadStatus.FAILED);
                req.setErrorMessage(e.getMessage());
                downloadRequestRepository.save(req);
            });
        }
    }

    /**
     * Create zip file for folder and all its contents
     */
    private String createZipFile(FileItem folder, String userId) throws IOException {
        // Create temp directory for zips
        Path zipDir = Paths.get(storageLocation, "zips", userId);
        Files.createDirectories(zipDir);

        // Generate unique zip filename
        String zipFilename = folder.getName() + "_" + UUID.randomUUID() + ".zip";
        Path zipPath = zipDir.resolve(zipFilename);

        try (FileOutputStream fos = new FileOutputStream(zipPath.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            // Add folder contents to zip
            addFolderToZip(folder, "", zos);
        }

        log.info("Created zip file: {}", zipPath);

        // Return relative path
        return "zips/" + userId + "/" + zipFilename;
    }

    /**
     * Recursively add folder contents to zip
     */
    private void addFolderToZip(FileItem folder, String parentPath, ZipOutputStream zos) throws IOException {
        List<FileItem> children = fileItemRepository.findByParentAndDeletedFalse(folder);

        for (FileItem child : children) {
            String currentPath = parentPath.isEmpty() ? child.getName() : parentPath + "/" + child.getName();

            if (child.getType() == FileType.FOLDER) {
                // Add folder entry
                ZipEntry folderEntry = new ZipEntry(currentPath + "/");
                zos.putNextEntry(folderEntry);
                zos.closeEntry();

                // Recursively add folder contents
                addFolderToZip(child, currentPath, zos);
            } else {
                // Add file
                ZipEntry fileEntry = new ZipEntry(currentPath);
                zos.putNextEntry(fileEntry);

                // Copy file content to zip
                Path filePath = Paths.get(storageLocation, child.getStoragePath());
                if (Files.exists(filePath)) {
                    try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, length);
                        }
                    }
                }

                zos.closeEntry();
            }
        }
    }

    /**
     * Get zip file for download
     */
    public File getZipFile(String requestId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        DownloadRequest request = downloadRequestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Download request not found"));

        // Check ownership
        if (!request.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to access this download");
        }

        if (request.getStatus() != DownloadStatus.READY) {
            throw new BadRequestException("Download is not ready yet");
        }

        Path zipPath = Paths.get(storageLocation, request.getDownloadPath());
        File zipFile = zipPath.toFile();

        if (!zipFile.exists()) {
            throw new ResourceNotFoundException("Zip file not found");
        }

        return zipFile;
    }
}
