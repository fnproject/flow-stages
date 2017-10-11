package com.example.fn;

public class TerminatedWithErrorException extends RuntimeException {
    public TerminatedWithErrorException(String message) {
        super(message);
    }
}
