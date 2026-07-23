package com.sanctuary.sanctuary_backend.exception;

public class DuplicateConfirmationException extends RuntimeException {
    public DuplicateConfirmationException(String message) {
        super(message);
    }
}