package com.fnproject.fn.flow.stages;

public class TerminatedWithErrorException extends RuntimeException {
    public TerminatedWithErrorException(String message) {
        super(message);
    }
}
