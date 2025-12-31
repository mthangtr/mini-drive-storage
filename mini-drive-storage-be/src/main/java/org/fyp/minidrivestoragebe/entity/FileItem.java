package org.fyp.minidrivestoragebe.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.minidrivestoragebe.enums.FileType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "file_items", indexes = {
    @Index(name = "idx_owner_parent", columnList = "owner_id, parent_id"),
    @Index(name = "idx_type", columnList = "type"),
    @Index(name = "idx_deleted", columnList = "deleted, deleted_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FileType type;
    
    @Column(length = 500)
    private String storagePath;
    
    @Column(length = 100)
    private String mimeType;
    
    @Column(nullable = false)
    @Builder.Default
    private Long size = 0L;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private FileItem parent;
    
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<FileItem> children = new HashSet<>();
    
    @OneToMany(mappedBy = "fileItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<FilePermission> permissions = new HashSet<>();
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
    
    @Column
    private LocalDateTime deletedAt;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
