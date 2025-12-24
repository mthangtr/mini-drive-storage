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
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final FileItemRepository fileItemRepository;
    private final FilePermissionRepository filePermissionRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;

    /**
     * Upload multiple files
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("isAuthenticated()")
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

    /**
     * Share a file or folder with another user
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ShareFileResponse shareFile(String fileId, ShareFileRequest request, String ownerEmail) {
        User owner = getUserByEmail(ownerEmail);
        FileItem fileItem = getFileItemById(fileId);
        
        // Check if user has permission to share (must be owner or have EDIT permission)
        checkWritePermission(fileItem, owner);
        
        // Find recipient user
        User recipient = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + request.getEmail() + " not found"));
        
        // Check if sharing with self
        if (owner.getId().equals(recipient.getId())) {
            throw new BadRequestException("Cannot share with yourself");
        }
        
        // Check if permission already exists
        FilePermission existingPermission = filePermissionRepository
                .findByFileItemAndUser(fileItem, recipient)
                .orElse(null);
        
        if (existingPermission != null) {
            // Update existing permission
            existingPermission.setPermissionLevel(request.getPermission());
            existingPermission = filePermissionRepository.save(existingPermission);
            
            log.info("Updated permission for {} on {} to {}", recipient.getEmail(), fileItem.getName(), request.getPermission());
        } else {
            // Create new permission
            existingPermission = FilePermission.builder()
                    .fileItem(fileItem)
                    .user(recipient)
                    .permissionLevel(request.getPermission())
                    .build();
            existingPermission = filePermissionRepository.save(existingPermission);
            
            log.info("Created permission for {} on {} with {}", recipient.getEmail(), fileItem.getName(), request.getPermission());
        }
        
        // If folder, apply permission recursively to all children
        if (fileItem.getType() == FileType.FOLDER) {
            applyPermissionsRecursively(fileItem, recipient, request.getPermission());
        }
        
        // Send email notification asynchronously
        emailService.sendShareNotification(
                recipient.getEmail(),
                owner.getEmail(),
                fileItem.getName(),
                request.getPermission().name()
        );
        
        return ShareFileResponse.builder()
                .id(existingPermission.getId())
                .fileId(fileItem.getId())
                .fileName(fileItem.getName())
                .sharedWithEmail(recipient.getEmail())
                .permission(existingPermission.getPermissionLevel())
                .sharedAt(existingPermission.getSharedAt())
                .build();
    }
    
    /**
     * Apply permissions recursively to all children in a folder
     */
    private void applyPermissionsRecursively(FileItem folder, User recipient, PermissionLevel permissionLevel) {
        List<FileItem> children = fileItemRepository.findByParentAndDeletedFalse(folder);
        
        for (FileItem child : children) {
            // Create or update permission for this child
            FilePermission childPermission = filePermissionRepository
                    .findByFileItemAndUser(child, recipient)
                    .orElse(null);
            
            if (childPermission != null) {
                childPermission.setPermissionLevel(permissionLevel);
                filePermissionRepository.save(childPermission);
            } else {
                childPermission = FilePermission.builder()
                        .fileItem(child)
                        .user(recipient)
                        .permissionLevel(permissionLevel)
                        .build();
                filePermissionRepository.save(childPermission);
            }
            
            // If child is also a folder, recurse
            if (child.getType() == FileType.FOLDER) {
                applyPermissionsRecursively(child, recipient, permissionLevel);
            }
        }
    }
    
    /**
     * Get list of files shared with current user
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<FileItemResponse> getSharedWithMe(String userEmail) {
        User user = getUserByEmail(userEmail);
        
        // Get all permissions for this user
        List<FilePermission> permissions = filePermissionRepository.findByUser(user);
        
        return permissions.stream()
                .map(permission -> {
                    FileItem fileItem = permission.getFileItem();
                    // Only include non-deleted files
                    if (!fileItem.getDeleted()) {
                        FileItemResponse response = FileItemResponse.from(fileItem, false);
                        // Mark as shared
                        response.setShared(true);
                        response.setPermissionLevel(permission.getPermissionLevel());
                        return response;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * Get list of users a file is shared with
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<ShareFileResponse> getFileShares(String fileId, String userEmail) {
        User user = getUserByEmail(userEmail);
        FileItem fileItem = getFileItemById(fileId);
        
        // Only owner can view who the file is shared with
        if (!fileItem.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("Only the owner can view file shares");
        }
        
        List<FilePermission> permissions = filePermissionRepository.findByFileItemId(fileId);
        
        return permissions.stream()
                .map(permission -> ShareFileResponse.builder()
                        .id(permission.getId())
                        .fileId(fileItem.getId())
                        .fileName(fileItem.getName())
                        .sharedWithEmail(permission.getUser().getEmail())
                        .permission(permission.getPermissionLevel())
                        .sharedAt(permission.getSharedAt())
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * Remove share (revoke permission)
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void removeShare(String fileId, String recipientEmail, String ownerEmail) {
        User owner = getUserByEmail(ownerEmail);
        FileItem fileItem = getFileItemById(fileId);
        
        // Only owner can remove shares
        if (!fileItem.getOwner().getId().equals(owner.getId())) {
            throw new UnauthorizedException("Only the owner can remove shares");
        }
        
        User recipient = getUserByEmail(recipientEmail);
        
        // Find and delete permission
        FilePermission permission = filePermissionRepository
                .findByFileItemAndUser(fileItem, recipient)
                .orElseThrow(() -> new ResourceNotFoundException("Share not found"));
        
        filePermissionRepository.delete(permission);
        
        // If folder, remove permissions recursively
        if (fileItem.getType() == FileType.FOLDER) {
            removePermissionsRecursively(fileItem, recipient);
        }
        
        log.info("Removed share for {} on {}", recipientEmail, fileItem.getName());
    }
    
    /**
     * Remove permissions recursively from all children
     */
    private void removePermissionsRecursively(FileItem folder, User recipient) {
        List<FileItem> children = fileItemRepository.findByParentAndDeletedFalse(folder);
        
        for (FileItem child : children) {
            filePermissionRepository.findByFileItemAndUser(child, recipient)
                    .ifPresent(filePermissionRepository::delete);
            
            if (child.getType() == FileType.FOLDER) {
                removePermissionsRecursively(child, recipient);
            }
        }
    }
}
