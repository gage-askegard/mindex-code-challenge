package com.mindex.challenge.exceptions;

/**
 * Custom exception class to flag an employee id that cannot be found, can be used to determine status code
 */
public class EmployeeNotFoundException extends Exception {
    public EmployeeNotFoundException(String message) {
        super(message);
    }
}
