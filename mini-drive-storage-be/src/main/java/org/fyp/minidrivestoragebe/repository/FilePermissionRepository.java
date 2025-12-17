package org.fyp.minidrivestoragebe.repository;

import org.fyp.minidrivestoragebe.entity.FileItem;
import org.fyp.minidrivestoragebe.entity.FilePermission;
import org.fyp.minidrivestoragebe.entity.User;
import org.fyp.minidrivestoragebe.enums.PermissionLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FilePermissionRepository extends JpaRepository<FilePermission, String> {
    
    // Find permission for specific user and file
    Optional<FilePermission> findByFileItemIdAndUserId(String fileItemId, String userId);
    
    // Find all permissions for a file
    List<FilePermission> findByFileItemId(String fileItemId);
    
    // Find all permissions for user
    List<FilePermission> findByUserId(String userId);
    
    List<FilePermission> findByUser(User user);
    
    Optional<FilePermission> findByFileItemAndUser(FileItem fileItem, User user);
    
    boolean existsByFileItemAndUser(FileItem fileItem, User user);
    
    // Check if permission exists
    boolean existsByFileItemIdAndUserId(String fileItemId, String userId);
    
    // Get user's permission level for a file
    @Query("SELECT p.permissionLevel FROM FilePermission p " +
           "WHERE p.fileItem.id = :fileItemId AND p.user.id = :userId")
    Optional<PermissionLevel> findPermissionLevel(
        @Param("fileItemId") String fileItemId, 
        @Param("userId") String userId);
    
    // Delete all permissions for a file
    void deleteByFileItemId(String fileItemId);
}
