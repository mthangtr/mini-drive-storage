package org.fyp.minidrivestoragebe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateSystemResponse {
    private Integer usersCreated;
    private Integer totalFilesCreated;
    private Integer totalFoldersCreated;
    private Integer sharingRelationsCreated;
    private Long executionTimeMs;
    private String message;
}
