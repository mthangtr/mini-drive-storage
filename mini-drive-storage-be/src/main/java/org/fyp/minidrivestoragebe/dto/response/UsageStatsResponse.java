package org.fyp.minidrivestoragebe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageStatsResponse {
    private Long storageUsed;
    private Long storageQuota;
    private Long storageAvailable;
    private Double usagePercentage;
    private Long totalFiles;
    private Long totalFolders;
    private Long totalSharedWithMe;
    private Long totalSharedByMe;
}
