package com.sanctuary.sanctuary_backend.exception;

public class InvalidGoogleTokenException extends RuntimeException {
    public InvalidGoogleTokenException() {
        super("Google sign-in could not be verified");
    }
}
