package io.restall.picodi;

public class UnexpectedPicodiException extends RuntimeException{
    public UnexpectedPicodiException(String message, Throwable cause) {
        super(message, cause);
    }
}
