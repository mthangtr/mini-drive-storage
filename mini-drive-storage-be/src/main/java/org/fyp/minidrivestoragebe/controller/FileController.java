package org.fyp.minidrivestoragebe.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Files & Folders", description = "File and folder management with unified endpoint")
@SecurityRequirement(name = "bearerAuth")
public class FileController {

    private final FileService fileService;
    private final DownloadService downloadService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload multiple files",
            description = "Upload one or more files to the storage. Optionally specify parentId to upload into a folder."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Files uploaded successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or parent is not a folder"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No write permission on parent folder")
    })
    public ResponseEntity<ApiResponse<UploadFileResponse>> uploadFiles(
            @Parameter(description = "Files to upload") @RequestParam("files") List<MultipartFile> files,
            @Parameter(description = "Parent folder ID (optional)") @RequestParam(required = false) String parentId,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        UploadFileResponse response = fileService.uploadFiles(files, parentId, userEmail);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Files uploaded successfully", response));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create folder", description = "Create a new folder. Set parentId to create nested folder.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Folder created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or duplicate folder name"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No write permission")
    })
    public ResponseEntity<ApiResponse<FileItemResponse>> createFolder(
            @Valid @RequestBody CreateFolderRequest request,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        FileItemResponse response = fileService.createFolder(request, userEmail);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Folder created successfully", response));
    }

    @GetMapping
    @Operation(summary = "List and search files", description = "List files/folders with optional search and filters. Searches both 'My Drive' and 'Shared with me'.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Files retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<List<FileItemResponse>>> listFiles(
            @Parameter(description = "Search keyword") @RequestParam(required = false) String q,
            @Parameter(description = "Filter by type: FILE or FOLDER") @RequestParam(required = false) String type,
            @Parameter(description = "Parent folder ID to list contents") @RequestParam(required = false) String parentId,
            @Parameter(description = "Minimum file size in bytes") @RequestParam(required = false) Long fromSize,
            @Parameter(description = "Maximum file size in bytes") @RequestParam(required = false) Long toSize,
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

    @GetMapping("/{id}")
    @Operation(summary = "Get file/folder details", description = "Get detailed information about a specific file or folder")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "File details retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No read permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "File not found")
    })
    public ResponseEntity<ApiResponse<FileItemResponse>> getFileDetails(
            @Parameter(description = "File or folder ID") @PathVariable String id,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        FileItemResponse response = fileService.getFileDetails(id, userEmail);
        
        return ResponseEntity.ok(ApiResponse.success("File details retrieved successfully", response));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download file (sync)", description = "Download a single file synchronously. Returns binary stream with appropriate Content-Type.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "File downloaded", content = @Content(mediaType = "application/octet-stream")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cannot download folder directly"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No read permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "File not found")
    })
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "File ID") @PathVariable String id,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        FileItemResponse fileDetails = fileService.getFileDetails(id, userEmail);
        Resource resource = fileService.downloadFile(id, userEmail);
        
        String contentType = fileDetails.getMimeType() != null 
                ? fileDetails.getMimeType() 
                : "application/octet-stream";
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + fileDetails.getName() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete file/folder (soft)", description = "Soft delete - moves to trash. Files in trash are permanently deleted after 30 days.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "File deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No write permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "File not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @Parameter(description = "File or folder ID") @PathVariable String id,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        fileService.deleteFile(id, userEmail);
        
        return ResponseEntity.ok(ApiResponse.success("File deleted successfully", null));
    }

    @PostMapping("/{id}/download")
    @Operation(summary = "Initiate folder download (async)", description = "Start async ZIP creation for folder. Returns requestId for polling status.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Download initiated, returns requestId"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Not a folder"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No read permission")
    })
    public ResponseEntity<ApiResponse<DownloadStatusResponse>> initiateFolderDownload(
            @Parameter(description = "Folder ID") @PathVariable String id,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        DownloadStatusResponse response = downloadService.initiateFolderDownload(id, userEmail);
        
        return ResponseEntity.ok(ApiResponse.success("Folder download initiated", response));
    }

    @GetMapping("/downloads/{requestId}")
    @Operation(summary = "Check download status (polling)", description = "Poll download status. Status: PENDING, PROCESSING, READY, FAILED")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Request not found")
    })
    public ResponseEntity<ApiResponse<DownloadStatusResponse>> getDownloadStatus(
            @Parameter(description = "Download request ID") @PathVariable String requestId,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        DownloadStatusResponse response = downloadService.getDownloadStatus(requestId, userEmail);
        
        return ResponseEntity.ok(ApiResponse.success("Download status retrieved", response));
    }

    @GetMapping("/downloads/{requestId}/file")
    @Operation(summary = "Download ZIP file", description = "Download the generated ZIP file when status is READY")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "ZIP file downloaded", content = @Content(mediaType = "application/zip")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Download not ready"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Request not found")
    })
    public ResponseEntity<Resource> downloadZipFile(
            @Parameter(description = "Download request ID") @PathVariable String requestId,
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

    @PostMapping("/{id}/share")
    @Operation(summary = "Share file/folder", description = "Share with another user. Permission: VIEW or EDIT. For folders, permission applies recursively to all children.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "File shared successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or sharing with self"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No permission to share"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "File or recipient not found")
    })
    public ResponseEntity<ApiResponse<ShareFileResponse>> shareFile(
            @Parameter(description = "File or folder ID") @PathVariable String id,
            @Valid @RequestBody ShareFileRequest request,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        ShareFileResponse response = fileService.shareFile(id, request, userEmail);
        
        return ResponseEntity.ok(ApiResponse.success("File shared successfully", response));
    }
    
    @GetMapping("/shared-with-me")
    @Operation(summary = "Get files shared with me", description = "List all files and folders that others have shared with current user")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shared files retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<List<FileItemResponse>>> getSharedWithMe(
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        List<FileItemResponse> files = fileService.getSharedWithMe(userEmail);
        
        return ResponseEntity.ok(ApiResponse.success("Shared files retrieved successfully", files));
    }
    
    @GetMapping("/{id}/shares")
    @Operation(summary = "Get file shares", description = "List all users a file/folder is shared with and their permission levels")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shares retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "File not found")
    })
    public ResponseEntity<ApiResponse<List<ShareFileResponse>>> getFileShares(
            @Parameter(description = "File or folder ID") @PathVariable String id,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        List<ShareFileResponse> shares = fileService.getFileShares(id, userEmail);
        
        return ResponseEntity.ok(ApiResponse.success("File shares retrieved successfully", shares));
    }
    
    @DeleteMapping("/{id}/share/{email}")
    @Operation(summary = "Remove share", description = "Revoke sharing permission for a specific user")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Share removed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No permission to remove share"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Share not found")
    })
    public ResponseEntity<ApiResponse<Void>> removeShare(
            @Parameter(description = "File or folder ID") @PathVariable String id,
            @Parameter(description = "Email of user to remove") @PathVariable String email,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        fileService.removeShare(id, email, userEmail);
        
        return ResponseEntity.ok(ApiResponse.success("Share removed successfully", null));
    }
}
