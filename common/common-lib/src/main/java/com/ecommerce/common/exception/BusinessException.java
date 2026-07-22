package com.ecommerce.common.exception;

/** Base class for expected, non-retryable business errors (validation, not-found, conflicts). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
    public BusinessException(String message, Throwable cause) { super(message, cause); }
}
