package com.codenvy.auth.sso.server;

/**
 * Exception used in BearerTokenManager to indicate that provided
 * bearer token doesn't exist or expired.
 * @author Sergii Kabashniuk
 */
public class InvalidBearerTokenException extends Exception {
    public InvalidBearerTokenException(String message) {
        super(message);
    }
}
