package org.fyp.minidrivestoragebe.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.minidrivestoragebe.dto.response.GenerateSystemResponse;
import org.fyp.minidrivestoragebe.entity.FileItem;
import org.fyp.minidrivestoragebe.entity.FilePermission;
import org.fyp.minidrivestoragebe.entity.User;
import org.fyp.minidrivestoragebe.enums.FileType;
import org.fyp.minidrivestoragebe.enums.PermissionLevel;
import org.fyp.minidrivestoragebe.repository.FileItemRepository;
import org.fyp.minidrivestoragebe.repository.FilePermissionRepository;
import org.fyp.minidrivestoragebe.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class DebugService {

    private final UserRepository userRepository;
    private final FileItemRepository fileItemRepository;
    private final FilePermissionRepository filePermissionRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int NUM_USERS = 10;
    private static final int TOTAL_FILES_TARGET = 10000;
    private static final int FILES_PER_USER = TOTAL_FILES_TARGET / NUM_USERS;
    private static final double SHARING_PERCENTAGE = 0.10; // 10%
    private static final String[] MIME_TYPES = {
        "application/pdf", "text/plain", "image/jpeg", "image/png",
        "application/vnd.ms-excel", "application/msword",
        "application/zip", "video/mp4"
    };
    private static final String[] FILE_EXTENSIONS = {
        ".pdf", ".txt", ".jpg", ".png", ".xlsx", ".docx", ".zip", ".mp4"
    };

    @Transactional
    public GenerateSystemResponse generateMockSystem() {
        long startTime = System.currentTimeMillis();
        log.info("Starting mock system generation...");

        // Delete existing data for clean generation
        cleanExistingData();

        List<User> users = createUsers();
        log.info("Created {} users", users.size());

        Map<User, List<FileItem>> userFilesMap = new HashMap<>();
        int totalFiles = 0;
        int totalFolders = 0;

        // Create file structure for each user
        for (User user : users) {
            List<FileItem> userFiles = createFileStructureForUser(user);
            userFilesMap.put(user, userFiles);
            
            long fileCount = userFiles.stream().filter(f -> f.getType() == FileType.FILE).count();
            long folderCount = userFiles.stream().filter(f -> f.getType() == FileType.FOLDER).count();
            
            totalFiles += fileCount;
            totalFolders += folderCount;
            
            log.info("Created {} files and {} folders for user: {}", fileCount, folderCount, user.getEmail());
        }

        // Create random sharing relationships
        int sharingRelations = createRandomSharing(users, userFilesMap);
        log.info("Created {} sharing relationships", sharingRelations);

        long executionTime = System.currentTimeMillis() - startTime;

        return GenerateSystemResponse.builder()
                .usersCreated(users.size())
                .totalFilesCreated(totalFiles)
                .totalFoldersCreated(totalFolders)
                .sharingRelationsCreated(sharingRelations)
                .executionTimeMs(executionTime)
                .message("Mock system generated successfully")
                .build();
    }

    private void cleanExistingData() {
        log.info("Cleaning existing data...");
        filePermissionRepository.deleteAll();
        fileItemRepository.deleteAll();
        
        // Keep admin user if exists, delete test users
        List<User> testUsers = userRepository.findAll().stream()
                .filter(u -> u.getEmail().startsWith("user") && u.getEmail().contains("@test.com"))
                .toList();
        userRepository.deleteAll(testUsers);
    }

    private List<User> createUsers() {
        List<User> users = new ArrayList<>();
        String defaultPassword = passwordEncoder.encode("password123");

        for (int i = 1; i <= NUM_USERS; i++) {
            User user = User.builder()
                    .email(String.format("user%d@test.com", i))
                    .password(defaultPassword)
                    .fullName(String.format("Test User %d", i))
                    .storageUsed(0L)
                    .storageQuota(10737418240L) // 10GB
                    .enabled(true)
                    .build();
            users.add(userRepository.save(user));
        }

        return users;
    }

    private List<FileItem> createFileStructureForUser(User user) {
        List<FileItem> allItems = new ArrayList<>();
        Random random = ThreadLocalRandom.current();

        // Create root folders (5-10 per user)
        int numRootFolders = 5 + random.nextInt(6);
        List<FileItem> rootFolders = new ArrayList<>();

        for (int i = 0; i < numRootFolders; i++) {
            FileItem folder = createFolder(user, null, "Folder_" + i);
            rootFolders.add(folder);
            allItems.add(folder);
        }

        // Calculate files to create per user
        int filesPerUser = FILES_PER_USER;
        int filesCreated = 0;

        // Distribute files across folders with some nesting
        for (FileItem rootFolder : rootFolders) {
            int filesInThisFolder = filesPerUser / numRootFolders;
            
            // Create some subfolders (1-3 per root folder)
            int numSubfolders = 1 + random.nextInt(3);
            List<FileItem> subfolders = new ArrayList<>();
            
            for (int i = 0; i < numSubfolders; i++) {
                FileItem subfolder = createFolder(user, rootFolder, "Subfolder_" + i);
                subfolders.add(subfolder);
                allItems.add(subfolder);
            }

            // Distribute files between root folder and subfolders
            int filesInRoot = filesInThisFolder / 2;
            int filesInSubfolders = filesInThisFolder - filesInRoot;

            // Files in root folder
            for (int i = 0; i < filesInRoot && filesCreated < filesPerUser; i++) {
                FileItem file = createFile(user, rootFolder, "File_" + filesCreated);
                allItems.add(file);
                filesCreated++;
            }

            // Files in subfolders
            if (!subfolders.isEmpty()) {
                int filesPerSubfolder = filesInSubfolders / subfolders.size();
                for (FileItem subfolder : subfolders) {
                    for (int i = 0; i < filesPerSubfolder && filesCreated < filesPerUser; i++) {
                        FileItem file = createFile(user, subfolder, "File_" + filesCreated);
                        allItems.add(file);
                        filesCreated++;
                    }
                }
            }
        }

        // Create remaining files in root level if needed
        while (filesCreated < filesPerUser) {
            FileItem randomFolder = rootFolders.get(random.nextInt(rootFolders.size()));
            FileItem file = createFile(user, randomFolder, "File_" + filesCreated);
            allItems.add(file);
            filesCreated++;
        }

        return allItems;
    }

    private FileItem createFolder(User owner, FileItem parent, String baseName) {
        String name = baseName + "_" + UUID.randomUUID().toString().substring(0, 8);
        
        FileItem folder = FileItem.builder()
                .name(name)
                .type(FileType.FOLDER)
                .size(0L)
                .owner(owner)
                .parent(parent)
                .deleted(false)
                .build();

        return fileItemRepository.save(folder);
    }

    private FileItem createFile(User owner, FileItem parent, String baseName) {
        Random random = ThreadLocalRandom.current();
        int typeIndex = random.nextInt(MIME_TYPES.length);
        String mimeType = MIME_TYPES[typeIndex];
        String extension = FILE_EXTENSIONS[typeIndex];
        
        String name = baseName + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        long size = 100000 + random.nextInt(10000000); // 100KB to 10MB

        // Create mock storage path (not actual file)
        String storagePath = String.format("mock/%s/%s", owner.getId(), name);

        FileItem file = FileItem.builder()
                .name(name)
                .type(FileType.FILE)
                .storagePath(storagePath)
                .mimeType(mimeType)
                .size(size)
                .owner(owner)
                .parent(parent)
                .deleted(false)
                .build();

        FileItem savedFile = fileItemRepository.save(file);

        // Update user storage
        owner.setStorageUsed(owner.getStorageUsed() + size);
        userRepository.save(owner);

        return savedFile;
    }

    private int createRandomSharing(List<User> users, Map<User, List<FileItem>> userFilesMap) {
        Random random = ThreadLocalRandom.current();
        int sharingRelations = 0;

        for (User owner : users) {
            List<FileItem> userFiles = userFilesMap.get(owner);
            if (userFiles.isEmpty()) continue;

            // Calculate 10% of user's files
            int numToShare = (int) Math.ceil(userFiles.size() * SHARING_PERCENTAGE);

            // Randomly select files to share
            Collections.shuffle(userFiles);
            List<FileItem> filesToShare = userFiles.subList(0, Math.min(numToShare, userFiles.size()));

            for (FileItem fileItem : filesToShare) {
                // Share with 1-3 random users
                int numUsersToShareWith = 1 + random.nextInt(3);
                List<User> otherUsers = users.stream()
                        .filter(u -> !u.getId().equals(owner.getId()))
                        .toList();

                Collections.shuffle(otherUsers);
                List<User> shareWithUsers = otherUsers.subList(0, Math.min(numUsersToShareWith, otherUsers.size()));

                for (User shareWith : shareWithUsers) {
                    // Random permission level
                    PermissionLevel permission = random.nextBoolean() 
                            ? PermissionLevel.VIEW 
                            : PermissionLevel.EDIT;

                    // Check if permission already exists
                    if (!filePermissionRepository.existsByFileItemAndUser(fileItem, shareWith)) {
                        FilePermission filePermission = FilePermission.builder()
                                .fileItem(fileItem)
                                .user(shareWith)
                                .permissionLevel(permission)
                                .build();
                        filePermissionRepository.save(filePermission);
                        sharingRelations++;
                    }
                }
            }
        }

        return sharingRelations;
    }
}
