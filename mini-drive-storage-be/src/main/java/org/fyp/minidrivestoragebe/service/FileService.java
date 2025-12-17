package org.fyp.minidrivestoragebe.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.minidrivestoragebe.dto.file.*;
import org.fyp.minidrivestoragebe.entity.FileItem;
import org.fyp.minidrivestoragebe.entity.FilePermission;
import org.fyp.minidrivestoragebe.entity.User;
import org.fyp.minidrivestoragebe.enums.FileType;
import org.fyp.minidrivestoragebe.enums.PermissionLevel;
import org.fyp.minidrivestoragebe.exception.BadRequestException;
import org.fyp.minidrivestoragebe.exception.ResourceNotFoundException;
import org.fyp.minidrivestoragebe.exception.UnauthorizedException;
import org.fyp.minidrivestoragebe.repository.FileItemRepository;
import org.fyp.minidrivestoragebe.repository.FilePermissionRepository;
import org.fyp.minidrivestoragebe.repository.UserRepository;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final FileItemRepository fileItemRepository;
    private final FilePermissionRepository filePermissionRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    /**
     * Upload multiple files
     */
    @Transactional
    public UploadFileResponse uploadFiles(List<MultipartFile> files, String parentId, String userEmail) {
        User user = getUserByEmail(userEmail);
        FileItem parent = null;

        // Validate parent folder if provided
        if (parentId != null && !parentId.isEmpty()) {
            parent = getFileItemById(parentId);
            
            // Check if parent is a folder
            if (parent.getType() != FileType.FOLDER) {
                throw new BadRequestException("Parent must be a folder");
            }
            
            // Check write permission
            checkWritePermission(parent, user);
        }

        List<FileItemResponse> uploadedFiles = new ArrayList<>();
        long totalSize = 0;

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            try {
                // Store physical file
                String storagePath = fileStorageService.storeFile(file, user.getId());
                long fileSize = file.getSize();

                // Create file metadata
                FileItem fileItem = FileItem.builder()
                        .name(file.getOriginalFilename())
                        .type(FileType.FILE)
                        .storagePath(storagePath)
                        .mimeType(file.getContentType())
                        .size(fileSize)
                        .owner(user)
                        .parent(parent)
                        .deleted(false)
                        .build();

                fileItem = fileItemRepository.save(fileItem);
                uploadedFiles.add(FileItemResponse.from(fileItem, true));
                totalSize += fileSize;

                log.info("File uploaded: {} by user: {}", fileItem.getName(), user.getEmail());
            } catch (Exception e) {
                log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
                // Continue with other files
            }
        }

        // Update user storage usage
        user.setStorageUsed(user.getStorageUsed() + totalSize);
        userRepository.save(user);

        return UploadFileResponse.builder()
                .files(uploadedFiles)
                .successCount(uploadedFiles.size())
                .totalCount(files.size())
                .build();
    }

    /**
     * Create a new folder
     */
    @Transactional
    public FileItemResponse createFolder(CreateFolderRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);
        FileItem parent = null;

        // Validate parent folder if provided
        if (request.getParentId() != null && !request.getParentId().isEmpty()) {
            parent = getFileItemById(request.getParentId());
            
            // Check if parent is a folder
            if (parent.getType() != FileType.FOLDER) {
                throw new BadRequestException("Parent must be a folder");
            }
            
            // Check write permission
            checkWritePermission(parent, user);
        }

        // Check for duplicate folder name in the same parent
        boolean exists = fileItemRepository.existsByNameAndParentAndOwnerAndDeletedFalse(
                request.getName(), parent, user);
        if (exists) {
            throw new BadRequestException("A folder with this name already exists in this location");
        }

        // Create folder
        FileItem folder = FileItem.builder()
                .name(request.getName())
                .type(FileType.FOLDER)
                .storagePath(null)
                .mimeType(null)
                .size(0L)
                .owner(user)
                .parent(parent)
                .deleted(false)
                .build();

        folder = fileItemRepository.save(folder);
        log.info("Folder created: {} by user: {}", folder.getName(), user.getEmail());

        return FileItemResponse.from(folder, true);
    }

    /**
     * List files and folders
     */
    @Transactional(readOnly = true)
    public List<FileItemResponse> listFiles(FileListRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);
        List<FileItem> items;

        if (request.getParentId() != null) {
            // List children of specific folder
            FileItem parent = getFileItemById(request.getParentId());
            checkReadPermission(parent, user);

            items = fileItemRepository.findByParentAndDeletedFalse(parent);
        } else if (request.getQ() != null && !request.getQ().isEmpty()) {
            // Global search
            items = searchFiles(request, user);
        } else {
            // List root level files
            items = fileItemRepository.findByOwnerAndParentIsNullAndDeletedFalse(user);
        }

        // Apply filters
        if (request.getType() != null) {
            items = items.stream()
                    .filter(item -> item.getType() == request.getType())
                    .collect(Collectors.toList());
        }

        if (request.getFromSize() != null) {
            items = items.stream()
                    .filter(item -> item.getSize() >= request.getFromSize())
                    .collect(Collectors.toList());
        }

        if (request.getToSize() != null) {
            items = items.stream()
                    .filter(item -> item.getSize() <= request.getToSize())
                    .collect(Collectors.toList());
        }

        return items.stream()
                .map(item -> {
                    boolean canEdit = canEdit(item, user);
                    return FileItemResponse.from(item, canEdit);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get file details by ID
     */
    @Transactional(readOnly = true)
    public FileItemResponse getFileDetails(String fileId, String userEmail) {
        User user = getUserByEmail(userEmail);
        FileItem fileItem = getFileItemById(fileId);
        
        checkReadPermission(fileItem, user);
        
        boolean canEdit = canEdit(fileItem, user);
        return FileItemResponse.from(fileItem, canEdit);
    }

    /**
     * Download file resource
     */
    @Transactional(readOnly = true)
    public Resource downloadFile(String fileId, String userEmail) {
        User user = getUserByEmail(userEmail);
        FileItem fileItem = getFileItemById(fileId);

        // Only files can be downloaded synchronously
        if (fileItem.getType() != FileType.FILE) {
            throw new BadRequestException("Only files can be downloaded directly. Use folder download endpoint for folders.");
        }

        checkReadPermission(fileItem, user);

        return fileStorageService.loadFileAsResource(fileItem.getStoragePath());
    }

    /**
     * Soft delete a file or folder
     */
    @Transactional
    public void deleteFile(String fileId, String userEmail) {
        User user = getUserByEmail(userEmail);
        FileItem fileItem = getFileItemById(fileId);

        checkWritePermission(fileItem, user);

        // Soft delete
        fileItem.setDeleted(true);
        fileItem.setDeletedAt(LocalDateTime.now());
        fileItemRepository.save(fileItem);

        log.info("File/Folder soft deleted: {} by user: {}", fileItem.getName(), user.getEmail());
    }

    /**
     * Search files across user's drive and shared files
     */
    private List<FileItem> searchFiles(FileListRequest request, User user) {
        String query = "%" + request.getQ().toLowerCase() + "%";
        
        // Search in owned files
        List<FileItem> ownedFiles = fileItemRepository.findByOwnerAndNameContainingIgnoreCaseAndDeletedFalse(
                user, request.getQ());

        // Search in shared files
        List<FilePermission> permissions = filePermissionRepository.findByUser(user);
        List<FileItem> sharedFiles = permissions.stream()
                .map(FilePermission::getFileItem)
                .filter(item -> !item.getDeleted())
                .filter(item -> item.getName().toLowerCase().contains(request.getQ().toLowerCase()))
                .collect(Collectors.toList());

        // Combine and remove duplicates
        List<FileItem> allFiles = new ArrayList<>(ownedFiles);
        for (FileItem sharedFile : sharedFiles) {
            if (!allFiles.contains(sharedFile)) {
                allFiles.add(sharedFile);
            }
        }

        return allFiles;
    }

    /**
     * Check if user has read permission on file/folder
     */
    private void checkReadPermission(FileItem fileItem, User user) {
        // Owner always has access
        if (fileItem.getOwner().getId().equals(user.getId())) {
            return;
        }

        // Check if user has permission
        boolean hasPermission = filePermissionRepository
                .existsByFileItemAndUser(fileItem, user);

        if (!hasPermission) {
            throw new UnauthorizedException("You don't have permission to access this resource");
        }
    }

    /**
     * Check if user has write permission on file/folder
     */
    private void checkWritePermission(FileItem fileItem, User user) {
        // Owner always has write access
        if (fileItem.getOwner().getId().equals(user.getId())) {
            return;
        }

        // Check if user has EDIT permission
        FilePermission permission = filePermissionRepository
                .findByFileItemAndUser(fileItem, user)
                .orElseThrow(() -> new UnauthorizedException("You don't have permission to modify this resource"));

        if (permission.getPermissionLevel() != PermissionLevel.EDIT) {
            throw new UnauthorizedException("You don't have write permission for this resource");
        }
    }

    /**
     * Check if user can edit the file/folder
     */
    private boolean canEdit(FileItem fileItem, User user) {
        // Owner always can edit
        if (fileItem.getOwner().getId().equals(user.getId())) {
            return true;
        }

        // Check permission level
        return filePermissionRepository.findByFileItemAndUser(fileItem, user)
                .map(p -> p.getPermissionLevel() == PermissionLevel.EDIT)
                .orElse(false);
    }

    /**
     * Helper: Get user by email
     */
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Helper: Get file item by ID
     */
    private FileItem getFileItemById(String id) {
        return fileItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File or folder not found"));
    }
}
