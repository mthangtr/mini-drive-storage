package org.fyp.minidrivestoragebe.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.minidrivestoragebe.enums.PermissionLevel;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_permissions", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"file_item_id", "user_id"}),
    indexes = {
        @Index(name = "idx_user_permission", columnList = "user_id"),
        @Index(name = "idx_file_permission", columnList = "file_item_id")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilePermission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_item_id", nullable = false)
    private FileItem fileItem;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PermissionLevel permissionLevel;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime sharedAt;
}
