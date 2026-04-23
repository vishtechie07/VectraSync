package com.vectrasync.core;

public class MissingCredentialException extends RuntimeException {
    public MissingCredentialException(String message) {
        super(message);
    }
}
