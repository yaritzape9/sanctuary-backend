package com.sanctuary.sanctuary_backend.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException() {
        super("An account with this email already exists. Sign in with your password.");
    }
}
