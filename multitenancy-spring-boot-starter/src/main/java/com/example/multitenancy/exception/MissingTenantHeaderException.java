package com.example.multitenancy.exception;

public class MissingTenantHeaderException extends RuntimeException {

    public MissingTenantHeaderException(String message) {
        super(message);
    }
}
