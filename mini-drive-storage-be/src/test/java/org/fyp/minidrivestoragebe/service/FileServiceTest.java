package org.fyp.minidrivestoragebe.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileService Unit Tests")
class FileServiceTest {

    @Mock
    private FileItemRepository fileItemRepository;

    @Mock
    private FilePermissionRepository filePermissionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private FileService fileService;

    private User testUser;
    private User otherUser;
    private FileItem testFolder;
    private FileItem testFile;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user-1")
                .email("test@example.com")
                .fullName("Test User")
                .storageUsed(0L)
                .storageQuota(10737418240L)
                .build();

        otherUser = User.builder()
                .id("user-2")
                .email("other@example.com")
                .fullName("Other User")
                .storageUsed(0L)
                .storageQuota(10737418240L)
                .build();

        testFolder = FileItem.builder()
                .id("folder-1")
                .name("Test Folder")
                .type(FileType.FOLDER)
                .owner(testUser)
                .size(0L)
                .deleted(false)
                .build();

        testFile = FileItem.builder()
                .id("file-1")
                .name("test.txt")
                .type(FileType.FILE)
                .storagePath("/storage/test.txt")
                .mimeType("text/plain")
                .size(1024L)
                .owner(testUser)
                .deleted(false)
                .build();
    }

    @Test
    @DisplayName("Should create folder successfully")
    void shouldCreateFolderSuccessfully() {
        // Given
        CreateFolderRequest request = CreateFolderRequest.builder()
                .name("New Folder")
                .parentId(null)
                .build();

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(fileItemRepository.existsByNameAndParentAndOwnerAndDeletedFalse(anyString(), any(), any(), anyBoolean()))
                .thenReturn(false);
        when(fileItemRepository.save(any(FileItem.class))).thenAnswer(i -> i.getArgument(0));

        // When
        FileItemResponse response = fileService.createFolder(request, testUser.getEmail());

        // Then
        assertNotNull(response);
        assertEquals("New Folder", response.getName());
        assertEquals(FileType.FOLDER, response.getType());
        verify(fileItemRepository, times(1)).save(any(FileItem.class));
    }

    @Test
    @DisplayName("Should throw exception when creating duplicate folder")
    void shouldThrowExceptionWhenCreatingDuplicateFolder() {
        // Given
        CreateFolderRequest request = CreateFolderRequest.builder()
                .name("Existing Folder")
                .parentId(null)
                .build();

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(fileItemRepository.existsByNameAndParentAndOwnerAndDeletedFalse(anyString(), any(), any(), anyBoolean()))
                .thenReturn(true);

        // When & Then
        assertThrows(BadRequestException.class, () ->
                fileService.createFolder(request, testUser.getEmail()));
        verify(fileItemRepository, never()).save(any(FileItem.class));
    }

    @Test
    @DisplayName("Should share file with VIEW permission")
    void shouldShareFileWithViewPermission() {
        // Given
        ShareFileRequest request = ShareFileRequest.builder()
                .email(otherUser.getEmail())
                .permission(PermissionLevel.VIEW)
                .build();

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail(otherUser.getEmail())).thenReturn(Optional.of(otherUser));
        when(fileItemRepository.findById(testFile.getId())).thenReturn(Optional.of(testFile));
        when(filePermissionRepository.findByFileItemAndUser(testFile, otherUser)).thenReturn(Optional.empty());
        when(filePermissionRepository.save(any(FilePermission.class))).thenAnswer(i -> {
            FilePermission permission = i.getArgument(0);
            permission.setId("permission-1");
            return permission;
        });

        // When
        ShareFileResponse response = fileService.shareFile(testFile.getId(), request, testUser.getEmail());

        // Then
        assertNotNull(response);
        assertEquals(otherUser.getEmail(), response.getSharedWithEmail());
        assertEquals(PermissionLevel.VIEW, response.getPermission());
        verify(filePermissionRepository, times(1)).save(any(FilePermission.class));
        verify(emailService, times(1)).sendShareNotification(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when sharing with self")
    void shouldThrowExceptionWhenSharingWithSelf() {
        // Given
        ShareFileRequest request = ShareFileRequest.builder()
                .email(testUser.getEmail())
                .permission(PermissionLevel.VIEW)
                .build();

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(fileItemRepository.findById(testFile.getId())).thenReturn(Optional.of(testFile));

        // When & Then
        assertThrows(BadRequestException.class, () ->
                fileService.shareFile(testFile.getId(), request, testUser.getEmail()));
        verify(filePermissionRepository, never()).save(any(FilePermission.class));
    }

    @Test
    @DisplayName("Should get file details when user is owner")
    void shouldGetFileDetailsWhenUserIsOwner() {
        // Given
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(fileItemRepository.findById(testFile.getId())).thenReturn(Optional.of(testFile));

        // When
        FileItemResponse response = fileService.getFileDetails(testFile.getId(), testUser.getEmail());

        // Then
        assertNotNull(response);
        assertEquals(testFile.getName(), response.getName());
        assertEquals(testFile.getType(), response.getType());
        assertTrue(response.getCanEdit());
    }

    @Test
    @DisplayName("Should get file details when user has permission")
    void shouldGetFileDetailsWhenUserHasPermission() {
        // Given
        FilePermission permission = FilePermission.builder()
                .fileItem(testFile)
                .user(otherUser)
                .permissionLevel(PermissionLevel.VIEW)
                .build();

        // Change owner to testUser so otherUser is not the owner
        testFile.setOwner(testUser);

        when(userRepository.findByEmail(otherUser.getEmail())).thenReturn(Optional.of(otherUser));
        when(fileItemRepository.findById(testFile.getId())).thenReturn(Optional.of(testFile));
        when(filePermissionRepository.existsByFileItemAndUser(testFile, otherUser)).thenReturn(true);
        when(filePermissionRepository.findByFileItemAndUser(testFile, otherUser)).thenReturn(Optional.of(permission));

        // When
        FileItemResponse response = fileService.getFileDetails(testFile.getId(), otherUser.getEmail());

        // Then
        assertNotNull(response);
        assertEquals(testFile.getName(), response.getName());
        assertFalse(response.getCanEdit()); // VIEW permission, not EDIT
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user has no permission")
    void shouldThrowUnauthorizedExceptionWhenUserHasNoPermission() {
        // Given
        testFile.setOwner(testUser);
        
        when(userRepository.findByEmail(otherUser.getEmail())).thenReturn(Optional.of(otherUser));
        when(fileItemRepository.findById(testFile.getId())).thenReturn(Optional.of(testFile));
        when(filePermissionRepository.existsByFileItemAndUser(testFile, otherUser)).thenReturn(false);

        // When & Then
        assertThrows(UnauthorizedException.class, () ->
                fileService.getFileDetails(testFile.getId(), otherUser.getEmail()));
    }

    @Test
    @DisplayName("Should soft delete file")
    void shouldSoftDeleteFile() {
        // Given
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(fileItemRepository.findById(testFile.getId())).thenReturn(Optional.of(testFile));
        when(fileItemRepository.save(any(FileItem.class))).thenAnswer(i -> i.getArgument(0));

        // When
        fileService.deleteFile(testFile.getId(), testUser.getEmail());

        // Then
        verify(fileItemRepository, times(1)).save(argThat(file ->
                file.getDeleted() && file.getDeletedAt() != null
        ));
    }

    @Test
    @DisplayName("Should get shared files")
    void shouldGetSharedFiles() {
        // Given
        FilePermission permission = FilePermission.builder()
                .fileItem(testFile)
                .user(otherUser)
                .permissionLevel(PermissionLevel.VIEW)
                .build();

        when(userRepository.findByEmail(otherUser.getEmail())).thenReturn(Optional.of(otherUser));
        when(filePermissionRepository.findByUser(otherUser)).thenReturn(List.of(permission));

        // When
        List<FileItemResponse> response = fileService.getSharedWithMe(otherUser.getEmail());

        // Then
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(testFile.getName(), response.get(0).getName());
        assertTrue(response.get(0).getShared());
    }

    @Test
    @DisplayName("Should remove share successfully")
    void shouldRemoveShareSuccessfully() {
        // Given
        FilePermission permission = FilePermission.builder()
                .id("permission-1")
                .fileItem(testFile)
                .user(otherUser)
                .permissionLevel(PermissionLevel.VIEW)
                .build();

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail(otherUser.getEmail())).thenReturn(Optional.of(otherUser));
        when(fileItemRepository.findById(testFile.getId())).thenReturn(Optional.of(testFile));
        when(filePermissionRepository.findByFileItemAndUser(testFile, otherUser)).thenReturn(Optional.of(permission));

        // When
        fileService.removeShare(testFile.getId(), otherUser.getEmail(), testUser.getEmail());

        // Then
        verify(filePermissionRepository, times(1)).delete(permission);
    }

    @Test
    @DisplayName("Should throw exception when non-owner tries to remove share")
    void shouldThrowExceptionWhenNonOwnerTriesToRemoveShare() {
        // Given
        testFile.setOwner(testUser);
        
        when(userRepository.findByEmail(otherUser.getEmail())).thenReturn(Optional.of(otherUser));
        when(fileItemRepository.findById(testFile.getId())).thenReturn(Optional.of(testFile));

        // When & Then
        assertThrows(UnauthorizedException.class, () ->
                fileService.removeShare(testFile.getId(), testUser.getEmail(), otherUser.getEmail()));
        verify(filePermissionRepository, never()).delete(any());
    }
}
