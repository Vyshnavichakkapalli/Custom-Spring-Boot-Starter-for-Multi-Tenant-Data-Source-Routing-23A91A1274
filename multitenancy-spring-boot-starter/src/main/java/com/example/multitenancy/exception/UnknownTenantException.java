package com.example.multitenancy.exception;

public class UnknownTenantException extends RuntimeException {

    public UnknownTenantException(String message) {
        super(message);
    }
}
