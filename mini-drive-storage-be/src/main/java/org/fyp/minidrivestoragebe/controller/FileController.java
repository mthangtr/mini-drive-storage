package org.fyp.minidrivestoragebe.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fyp.minidrivestoragebe.dto.file.*;
import org.fyp.minidrivestoragebe.dto.response.ApiResponse;
import org.fyp.minidrivestoragebe.service.DownloadService;
import org.fyp.minidrivestoragebe.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final DownloadService downloadService;

    /**
     * Upload multiple files or create folder
     * Content-Type: multipart/form-data -> Upload files
     * Content-Type: application/json -> Create folder
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadFileResponse>> uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(required = false) String parentId,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        UploadFileResponse response = fileService.uploadFiles(files, parentId, userEmail);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Files uploaded successfully", response));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<FileItemResponse>> createFolder(
            @Valid @RequestBody CreateFolderRequest request,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        FileItemResponse response = fileService.createFolder(request, userEmail);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Folder created successfully", response));
    }

    /**
     * List files and folders with optional filters
     * GET /api/v1/files?q=search&type=FILE&parentId=xxx&fromSize=0&toSize=1000
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FileItemResponse>>> listFiles(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String parentId,
            @RequestParam(required = false) Long fromSize,
            @RequestParam(required = false) Long toSize,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        
        FileListRequest request = FileListRequest.builder()
                .q(q)
                .type(type != null ? org.fyp.minidrivestoragebe.enums.FileType.valueOf(type) : null)
                .parentId(parentId)
                .fromSize(fromSize)
                .toSize(toSize)
                .build();
        
        List<FileItemResponse> files = fileService.listFiles(request, userEmail);
        
        return ResponseEntity.ok(ApiResponse.success("Files retrieved successfully", files));
    }

    /**
     * Get file details by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FileItemResponse>> getFileDetails(
            @PathVariable String id,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        FileItemResponse response = fileService.getFileDetails(id, userEmail);
        
        return ResponseEntity.ok(ApiResponse.success("File details retrieved successfully", response));
    }

    /**
     * Download file (sync)
     * GET /api/v1/files/{id}/download
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String id,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        
        // Get file details first
        FileItemResponse fileDetails = fileService.getFileDetails(id, userEmail);
        
        // Download the file
        Resource resource = fileService.downloadFile(id, userEmail);
        
        // Set headers for file download
        String contentType = fileDetails.getMimeType() != null 
                ? fileDetails.getMimeType() 
                : "application/octet-stream";
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + fileDetails.getName() + "\"")
                .body(resource);
    }

    /**
     * Delete file or folder (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @PathVariable String id,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        fileService.deleteFile(id, userEmail);
        
        return ResponseEntity.ok(ApiResponse.success("File deleted successfully", null));
    }

    /**
     * Initiate folder download (async)
     * POST /api/v1/files/{id}/download
     */
    @PostMapping("/{id}/download")
    public ResponseEntity<ApiResponse<DownloadStatusResponse>> initiateFolderDownload(
            @PathVariable String id,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        DownloadStatusResponse response = downloadService.initiateFolderDownload(id, userEmail);
        
        return ResponseEntity.ok(ApiResponse.success("Folder download initiated", response));
    }

    /**
     * Get download status (polling endpoint)
     * GET /api/v1/files/downloads/{requestId}
     */
    @GetMapping("/downloads/{requestId}")
    public ResponseEntity<ApiResponse<DownloadStatusResponse>> getDownloadStatus(
            @PathVariable String requestId,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        DownloadStatusResponse response = downloadService.getDownloadStatus(requestId, userEmail);
        
        return ResponseEntity.ok(ApiResponse.success("Download status retrieved", response));
    }

    /**
     * Download the zip file
     * GET /api/v1/files/downloads/{requestId}/file
     */
    @GetMapping("/downloads/{requestId}/file")
    public ResponseEntity<Resource> downloadZipFile(
            @PathVariable String requestId,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        java.io.File zipFile = downloadService.getZipFile(requestId, userEmail);
        
        Resource resource = new FileSystemResource(zipFile);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + zipFile.getName() + "\"")
                .body(resource);
    }
}
