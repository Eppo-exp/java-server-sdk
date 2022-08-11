package com.eppo.sdk.exception;

public class EppoClientIsNotInitializedException extends RuntimeException {
    public EppoClientIsNotInitializedException(String message) {
        super(message);
    }
}
