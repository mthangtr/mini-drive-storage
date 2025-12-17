package org.fyp.minidrivestoragebe.dto.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.minidrivestoragebe.entity.FileItem;
import org.fyp.minidrivestoragebe.enums.FileType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileItemResponse {
    private String id;
    private String name;
    private FileType type;
    private String mimeType;
    private Long size;
    private String parentId;
    private String ownerId;
    private String ownerName;
    private Boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean canEdit; // Permission flag

    public static FileItemResponse from(FileItem fileItem, Boolean canEdit) {
        return FileItemResponse.builder()
                .id(fileItem.getId())
                .name(fileItem.getName())
                .type(fileItem.getType())
                .mimeType(fileItem.getMimeType())
                .size(fileItem.getSize())
                .parentId(fileItem.getParent() != null ? fileItem.getParent().getId() : null)
                .ownerId(fileItem.getOwner().getId())
                .ownerName(fileItem.getOwner().getFullName())
                .deleted(fileItem.getDeleted())
                .createdAt(fileItem.getCreatedAt())
                .updatedAt(fileItem.getUpdatedAt())
                .canEdit(canEdit)
                .build();
    }
}
