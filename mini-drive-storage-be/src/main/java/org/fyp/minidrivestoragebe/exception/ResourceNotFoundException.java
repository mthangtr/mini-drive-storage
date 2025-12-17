package org.fyp.minidrivestoragebe.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException fileNotFound(String fileId) {
        return new ResourceNotFoundException("File not found with id: " + fileId);
    }

    public static ResourceNotFoundException userNotFound(String userId) {
        return new ResourceNotFoundException("User not found with id: " + userId);
    }
}
