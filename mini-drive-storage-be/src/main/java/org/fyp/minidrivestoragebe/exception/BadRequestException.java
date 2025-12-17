package org.fyp.minidrivestoragebe.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }

    public static BadRequestException emailAlreadyExists() {
        return new BadRequestException("Email already exists");
    }

    public static BadRequestException invalidFileType() {
        return new BadRequestException("Invalid file type");
    }

    public static BadRequestException storageQuotaExceeded() {
        return new BadRequestException("Storage quota exceeded");
    }

    public static BadRequestException emptyFile() {
        return new BadRequestException("Cannot upload empty file");
    }
}
