package com.sanctuary.sanctuary_backend.exception;

public class UnauthorizedSightingActionException extends RuntimeException {
    public UnauthorizedSightingActionException(String message) {
        super(message);
    }
}