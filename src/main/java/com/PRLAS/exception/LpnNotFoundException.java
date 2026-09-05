package com.PRLAS.exception;

public class LpnNotFoundException extends RuntimeException {
    public LpnNotFoundException(String message) {
        super(message);
    }
}