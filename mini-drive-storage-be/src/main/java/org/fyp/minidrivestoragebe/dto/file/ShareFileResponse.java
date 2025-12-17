package org.fyp.minidrivestoragebe.dto.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.minidrivestoragebe.enums.PermissionLevel;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareFileResponse {
    
    private String id;
    private String fileId;
    private String fileName;
    private String sharedWithEmail;
    private PermissionLevel permission;
    private LocalDateTime sharedAt;
}
