package org.fyp.minidrivestoragebe.exception;

public class FileStorageException extends RuntimeException {
    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public static FileStorageException uploadFailed(String filename) {
        return new FileStorageException("Failed to upload file: " + filename);
    }

    public static FileStorageException downloadFailed(String filename) {
        return new FileStorageException("Failed to download file: " + filename);
    }

    public static FileStorageException deleteFailed(String filename) {
        return new FileStorageException("Failed to delete file: " + filename);
    }
}
