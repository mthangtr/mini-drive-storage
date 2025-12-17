package org.fyp.minidrivestoragebe.dto.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.minidrivestoragebe.enums.FileType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileListRequest {
    private String q; // Search query
    private FileType type; // Filter by type
    private String parentId; // List children of this folder
    private Long fromSize; // Min size filter
    private Long toSize; // Max size filter
}
