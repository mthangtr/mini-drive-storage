package org.fyp.minidrivestoragebe.dto.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.minidrivestoragebe.enums.DownloadStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadStatusResponse {
    private String requestId;
    private DownloadStatus status;
    private String downloadUrl;
    private String message;
    
    public static DownloadStatusResponse of(String requestId, DownloadStatus status, String downloadUrl, String message) {
        return DownloadStatusResponse.builder()
                .requestId(requestId)
                .status(status)
                .downloadUrl(downloadUrl)
                .message(message)
                .build();
    }
}
