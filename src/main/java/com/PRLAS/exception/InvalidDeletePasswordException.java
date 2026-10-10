package com.PRLAS.exception;

public class InvalidDeletePasswordException extends RuntimeException {
    public InvalidDeletePasswordException(String message) {
        super(message);
    }
}
