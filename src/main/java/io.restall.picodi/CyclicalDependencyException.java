package io.restall.picodi;

public class CyclicalDependencyException extends RuntimeException {

    public CyclicalDependencyException(String message, Object... formatArgs) {
        super(String.format(message, formatArgs));
    }
}
