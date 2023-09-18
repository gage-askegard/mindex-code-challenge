package com.mindex.challenge.exceptions;

/**
 * Custom exception class to flag that an entity could not be found, can be used to determine api error handling
 */
public class NotFoundException extends Exception {
    public NotFoundException(String message) {
        super(message);
    }
}
