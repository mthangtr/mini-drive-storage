package org.fyp.minidrivestoragebe.service;

import lombok.extern.slf4j.Slf4j;
import org.fyp.minidrivestoragebe.exception.FileStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Service for handling physical file storage operations on disk
 */
@Service
@Slf4j
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${app.storage.location}") String storageLocation) {
        this.fileStorageLocation = Paths.get(storageLocation).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("File storage location initialized at: {}", this.fileStorageLocation);
        } catch (IOException e) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", e);
        }
    }

    /**
     * Store a file and return the storage path
     */
    public String storeFile(MultipartFile file, String ownerId) {
        // Normalize file name
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        
        try {
            // Check if file name contains invalid characters
            if (originalFilename.contains("..")) {
                throw new FileStorageException("Filename contains invalid path sequence: " + originalFilename);
            }

            // Generate unique filename to avoid collisions
            String extension = "";
            int lastDotIndex = originalFilename.lastIndexOf('.');
            if (lastDotIndex > 0) {
                extension = originalFilename.substring(lastDotIndex);
            }
            String uniqueFilename = UUID.randomUUID().toString() + extension;

            // Create user-specific directory
            Path userDirectory = this.fileStorageLocation.resolve(ownerId);
            Files.createDirectories(userDirectory);

            // Copy file to the target location
            Path targetLocation = userDirectory.resolve(uniqueFilename);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            // Return relative path for database storage
            return ownerId + "/" + uniqueFilename;

        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + originalFilename + ". Please try again!", ex);
        }
    }

    /**
     * Load file as Resource
     */
    public Resource loadFileAsResource(String storagePath) {
        try {
            Path filePath = this.fileStorageLocation.resolve(storagePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileStorageException("File not found: " + storagePath);
            }
        } catch (MalformedURLException ex) {
            throw new FileStorageException("File not found: " + storagePath, ex);
        }
    }

    /**
     * Delete a physical file
     */
    public void deleteFile(String storagePath) {
        try {
            Path filePath = this.fileStorageLocation.resolve(storagePath).normalize();
            Files.deleteIfExists(filePath);
            log.debug("Deleted file: {}", storagePath);
        } catch (IOException ex) {
            log.error("Could not delete file: {}", storagePath, ex);
            // Don't throw exception, just log it (file might already be deleted)
        }
    }

    /**
     * Get file size
     */
    public long getFileSize(String storagePath) {
        try {
            Path filePath = this.fileStorageLocation.resolve(storagePath).normalize();
            return Files.size(filePath);
        } catch (IOException ex) {
            log.error("Could not get file size: {}", storagePath, ex);
            return 0;
        }
    }

    /**
     * Check if file exists
     */
    public boolean fileExists(String storagePath) {
        try {
            Path filePath = this.fileStorageLocation.resolve(storagePath).normalize();
            return Files.exists(filePath);
        } catch (Exception ex) {
            return false;
        }
    }
}
