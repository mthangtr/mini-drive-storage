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

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public UploadFileResponse uploadFiles(List<MultipartFile> files, String parentId, String userEmail) {
        User user = getUserByEmail(userEmail);
        FileItem parent = null;

        if (parentId != null && !parentId.isEmpty()) {
            parent = getFileItemById(parentId);
            
            if (parent.getType() != FileType.FOLDER) {
                throw new BadRequestException("Parent must be a folder");
            }
            
            checkWritePermission(parent, user);
        }

        List<FileItemResponse> uploadedFiles = new ArrayList<>();
        long totalSize = 0;

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            try {
                String storagePath = fileStorageService.storeFile(file, user.getId());
                long fileSize = file.getSize();

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

                log.info("File uploaded: {}", fileItem.getName());
            } catch (Exception e) {
                log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            }
        }

        user.setStorageUsed(user.getStorageUsed() + totalSize);
        userRepository.save(user);

        return UploadFileResponse.builder()
                .files(uploadedFiles)
                .successCount(uploadedFiles.size())
                .totalCount(files.size())
                .build();
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public FileItemResponse createFolder(CreateFolderRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);
        FileItem parent = null;

        if (request.getParentId() != null && !request.getParentId().isEmpty()) {
            parent = getFileItemById(request.getParentId());
            
            if (parent.getType() != FileType.FOLDER) {
                throw new BadRequestException("Parent must be a folder");
            }
            
            checkWritePermission(parent, user);
        }

        boolean exists = fileItemRepository.existsByNameAndParentAndOwnerAndDeletedFalse(
                request.getName(), parent, user);
        if (exists) {
            throw new BadRequestException("A folder with this name already exists in this location");
        }

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
        log.info("Folder created: {}", folder.getName());

        return FileItemResponse.from(folder, true);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<FileItemResponse> listFiles(FileListRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);
        List<FileItem> items;

        if (request.getParentId() != null) {
            FileItem parent = getFileItemById(request.getParentId());
            checkReadPermission(parent, user);

            items = fileItemRepository.findByParentAndDeletedFalse(parent);
        } else if (request.getQ() != null && !request.getQ().isEmpty()) {
            items = searchFiles(request, user);
        } else {
            items = fileItemRepository.findByOwnerAndParentIsNullAndDeletedFalse(user);
        }

        List<FileItem> filteredItems = new ArrayList<>();
        for (FileItem item : items) {
            boolean matches = true;
            
            if (request.getType() != null && item.getType() != request.getType()) {
                matches = false;
            }
            if (request.getFromSize() != null && item.getSize() < request.getFromSize()) {
                matches = false;
            }
            if (request.getToSize() != null && item.getSize() > request.getToSize()) {
                matches = false;
            }
            
            if (matches) {
                filteredItems.add(item);
            }
        }
        items = filteredItems;

        List<FileItemResponse> result = new ArrayList<>();
        for (FileItem item : items) {
            boolean canEdit = canEdit(item, user);
            result.add(FileItemResponse.from(item, canEdit));
        }
        return result;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public FileItemResponse getFileDetails(String fileId, String userEmail) {
        User user = getUserByEmail(userEmail);
        FileItem fileItem = getFileItemById(fileId);
        
        checkReadPermission(fileItem, user);
        
        boolean canEdit = canEdit(fileItem, user);
        return FileItemResponse.from(fileItem, canEdit);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Resource downloadFile(String fileId, String userEmail) {
        User user = getUserByEmail(userEmail);
        FileItem fileItem = getFileItemById(fileId);

        if (fileItem.getType() != FileType.FILE) {
            throw new BadRequestException("Only files can be downloaded directly. Use folder download endpoint for folders.");
        }

        checkReadPermission(fileItem, user);

        return fileStorageService.loadFileAsResource(fileItem.getStoragePath());
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void deleteFile(String fileId, String userEmail) {
        User user = getUserByEmail(userEmail);
        FileItem fileItem = getFileItemById(fileId);

        checkWritePermission(fileItem, user);

        fileItem.setDeleted(true);
        fileItem.setDeletedAt(LocalDateTime.now());
        fileItemRepository.save(fileItem);

        log.info("Deleted: {}", fileItem.getName());
    }

    private List<FileItem> searchFiles(FileListRequest request, User user) {
        String query = "%" + request.getQ().toLowerCase() + "%";
        
        List<FileItem> ownedFiles = fileItemRepository.findByOwnerAndNameContainingIgnoreCaseAndDeletedFalse(
                user, request.getQ());

        List<FilePermission> permissions = filePermissionRepository.findByUser(user);
        List<FileItem> sharedFiles = new ArrayList<>();
        for (FilePermission perm : permissions) {
            FileItem item = perm.getFileItem();
            if (!item.getDeleted() && item.getName().toLowerCase().contains(request.getQ().toLowerCase())) {
                sharedFiles.add(item);
            }
        }

        List<FileItem> allFiles = new ArrayList<>(ownedFiles);
        for (FileItem sharedFile : sharedFiles) {
            if (!allFiles.contains(sharedFile)) {
                allFiles.add(sharedFile);
            }
        }

        return allFiles;
    }

    private void checkReadPermission(FileItem fileItem, User user) {
        if (fileItem.getOwner().getId().equals(user.getId())) {
            return;
        }

        boolean hasPermission = filePermissionRepository
                .existsByFileItemAndUser(fileItem, user);

        if (!hasPermission) {
            throw new UnauthorizedException("You don't have permission to access this resource");
        }
    }

    private void checkWritePermission(FileItem fileItem, User user) {
        if (fileItem.getOwner().getId().equals(user.getId())) {
            return;
        }

        FilePermission permission = filePermissionRepository
                .findByFileItemAndUser(fileItem, user)
                .orElseThrow(() -> new UnauthorizedException("You don't have permission to modify this resource"));

        if (permission.getPermissionLevel() != PermissionLevel.EDIT) {
            throw new UnauthorizedException("You don't have write permission for this resource");
        }
    }

    private boolean canEdit(FileItem fileItem, User user) {
        if (fileItem.getOwner().getId().equals(user.getId())) {
            return true;
        }

        return filePermissionRepository.findByFileItemAndUser(fileItem, user)
                .map(p -> p.getPermissionLevel() == PermissionLevel.EDIT)
                .orElse(false);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private FileItem getFileItemById(String id) {
        return fileItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File or folder not found"));
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ShareFileResponse shareFile(String fileId, ShareFileRequest request, String ownerEmail) {
        User owner = getUserByEmail(ownerEmail);
        FileItem fileItem = getFileItemById(fileId);
        
        checkWritePermission(fileItem, owner);
        
        User recipient = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + request.getEmail() + " not found"));
        
        if (owner.getId().equals(recipient.getId())) {
            throw new BadRequestException("Cannot share with yourself");
        }
        
        FilePermission existingPermission = filePermissionRepository
                .findByFileItemAndUser(fileItem, recipient)
                .orElse(null);
        
        if (existingPermission != null) {
            existingPermission.setPermissionLevel(request.getPermission());
            existingPermission = filePermissionRepository.save(existingPermission);
            
            log.info("Permission updated for {}", recipient.getEmail());
        } else {
            existingPermission = FilePermission.builder()
                    .fileItem(fileItem)
                    .user(recipient)
                    .permissionLevel(request.getPermission())
                    .build();
            existingPermission = filePermissionRepository.save(existingPermission);
            
            log.info("Permission created for {}", recipient.getEmail());
        }
        
        if (fileItem.getType() == FileType.FOLDER) {
            applyPermissionsRecursively(fileItem, recipient, request.getPermission());
        }
        
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
    
    private void applyPermissionsRecursively(FileItem folder, User recipient, PermissionLevel permissionLevel) {
        List<FileItem> children = fileItemRepository.findByParentAndDeletedFalse(folder);
        
        for (FileItem child : children) {
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
            
            if (child.getType() == FileType.FOLDER) {
                applyPermissionsRecursively(child, recipient, permissionLevel);
            }
        }
    }
    
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<FileItemResponse> getSharedWithMe(String userEmail) {
        User user = getUserByEmail(userEmail);
        
        List<FilePermission> permissions = filePermissionRepository.findByUser(user);
        
        List<FileItemResponse> responses = new ArrayList<>();
        for (FilePermission permission : permissions) {
            FileItem fileItem = permission.getFileItem();
            if (!fileItem.getDeleted()) {
                FileItemResponse response = FileItemResponse.from(fileItem, false);
                response.setShared(true);
                response.setPermissionLevel(permission.getPermissionLevel());
                responses.add(response);
            }
        }
        return responses;
    }
    
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<ShareFileResponse> getFileShares(String fileId, String userEmail) {
        User user = getUserByEmail(userEmail);
        FileItem fileItem = getFileItemById(fileId);
        
        if (!fileItem.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("Only the owner can view file shares");
        }
        
        List<FilePermission> permissions = filePermissionRepository.findByFileItemId(fileId);
        
        List<ShareFileResponse> responses = new ArrayList<>();
        for (FilePermission permission : permissions) {
            ShareFileResponse response = ShareFileResponse.builder()
                    .id(permission.getId())
                    .fileId(fileItem.getId())
                    .fileName(fileItem.getName())
                    .sharedWithEmail(permission.getUser().getEmail())
                    .permission(permission.getPermissionLevel())
                    .sharedAt(permission.getSharedAt())
                    .build();
            responses.add(response);
        }
        return responses;
    }
    
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void removeShare(String fileId, String recipientEmail, String ownerEmail) {
        User owner = getUserByEmail(ownerEmail);
        FileItem fileItem = getFileItemById(fileId);
        
        if (!fileItem.getOwner().getId().equals(owner.getId())) {
            throw new UnauthorizedException("Only the owner can remove shares");
        }
        
        User recipient = getUserByEmail(recipientEmail);
        
        FilePermission permission = filePermissionRepository
                .findByFileItemAndUser(fileItem, recipient)
                .orElseThrow(() -> new ResourceNotFoundException("Share not found"));
        
        filePermissionRepository.delete(permission);
        
        if (fileItem.getType() == FileType.FOLDER) {
            removePermissionsRecursively(fileItem, recipient);
        }
        
        log.info("Share removed");
    }
    
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
