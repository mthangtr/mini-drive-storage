package org.fyp.minidrivestoragebe.dto.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadFileResponse {
    private List<FileItemResponse> files;
    private Integer successCount;
    private Integer totalCount;
}
