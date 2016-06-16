package com.codenvy.auth.sso.server;

/**
 * Exception used in BearerTokenManager to indicate that provided
 * bearer token doesn't exist or expired.
 * @author Sergii Kabashniuk
 */
public class InvalidBearerTokenException extends Exception {
    private final String token;


    public InvalidBearerTokenException(String message, String token) {
        super(message);
        this.token = token;
    }


    public String getToken() {
        return token;
    }
}
