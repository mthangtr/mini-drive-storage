package org.fyp.minidrivestoragebe.repository;

import org.fyp.minidrivestoragebe.entity.FileItem;
import org.fyp.minidrivestoragebe.entity.User;
import org.fyp.minidrivestoragebe.enums.FileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileItemRepository extends JpaRepository<FileItem, String> {
    
    // Find by owner and parent (listing files in a folder)
    List<FileItem> findByOwnerIdAndParentIdAndDeletedFalse(String ownerId, String parentId);
    
    // Find root level items (no parent)
    List<FileItem> findByOwnerIdAndParentIsNullAndDeletedFalse(String ownerId);
    
    List<FileItem> findByOwnerAndParentIsNullAndDeletedFalse(User owner);
    
    List<FileItem> findByParentAndDeletedFalse(FileItem parent);
    
    List<FileItem> findByOwnerAndNameContainingIgnoreCaseAndDeletedFalse(User owner, String name);
    
    boolean existsByNameAndParentAndOwnerAndDeletedFalse(String name, FileItem parent, User owner);
    
    // Search files by name
    @Query("SELECT f FROM FileItem f WHERE f.owner.id = :ownerId AND f.deleted = false " +
           "AND LOWER(f.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<FileItem> searchByOwnerAndName(@Param("ownerId") String ownerId, @Param("query") String query);
    
    // Find files shared with user
    @Query("SELECT DISTINCT f FROM FileItem f " +
           "JOIN f.permissions p " +
           "WHERE p.user.id = :userId AND f.deleted = false")
    List<FileItem> findSharedWithUser(@Param("userId") String userId);
    
    // Find by owner, parent and type
    List<FileItem> findByOwnerIdAndParentIdAndTypeAndDeletedFalse(
        String ownerId, String parentId, FileType type);
    
    // Find with owner check
    Optional<FileItem> findByIdAndOwnerId(String id, String ownerId);
    
    // Find deleted items older than date
    List<FileItem> findByDeletedTrueAndDeletedAtBefore(LocalDateTime dateTime);
    
    // Find all children recursively (for deleting folder)
    @Query("SELECT f FROM FileItem f WHERE f.parent.id = :parentId")
    List<FileItem> findAllByParentId(@Param("parentId") String parentId);
    
    // Check if user has access (owner or has permission)
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FileItem f " +
           "LEFT JOIN f.permissions p " +
           "WHERE f.id = :fileId AND f.deleted = false " +
           "AND (f.owner.id = :userId OR p.user.id = :userId)")
    boolean hasAccess(@Param("fileId") String fileId, @Param("userId") String userId);
}
