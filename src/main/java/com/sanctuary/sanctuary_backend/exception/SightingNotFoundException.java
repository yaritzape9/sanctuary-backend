package com.sanctuary.sanctuary_backend.exception;

public class SightingNotFoundException extends RuntimeException {
    public SightingNotFoundException(String message) {
        super(message);
    }
}