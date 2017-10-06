package com.example.fn;

public class InvalidMachineException extends RuntimeException {
    public InvalidMachineException(String message) {
        super(message);
    }
}
