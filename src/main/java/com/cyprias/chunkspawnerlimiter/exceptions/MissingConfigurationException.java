package com.cyprias.chunkspawnerlimiter.exceptions;

/**
 * Thrown when a required configuration is missing or invalid.
 */
public class MissingConfigurationException extends RuntimeException {

    public MissingConfigurationException(String message) {
        super(message);
    }

    public MissingConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingConfigurationException(Throwable cause) {
        super(cause);
    }
}
