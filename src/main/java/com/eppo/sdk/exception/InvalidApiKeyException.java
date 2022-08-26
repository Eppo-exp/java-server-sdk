package com.eppo.sdk.exception;

public class InvalidApiKeyException extends RuntimeException {
  public InvalidApiKeyException(String message) {
    super(message);
  }
}
