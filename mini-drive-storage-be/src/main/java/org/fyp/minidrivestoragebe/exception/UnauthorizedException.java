package org.fyp.minidrivestoragebe.exception;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }

    public static UnauthorizedException accessDenied() {
        return new UnauthorizedException("You don't have permission to access this resource");
    }

    public static UnauthorizedException invalidCredentials() {
        return new UnauthorizedException("Invalid email or password");
    }
}
