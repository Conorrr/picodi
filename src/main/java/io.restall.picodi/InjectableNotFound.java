package io.restall.picodi;

public class InjectableNotFound extends RuntimeException {
    public InjectableNotFound(String message, Object... formatArgs) {
        super(String.format(message, formatArgs));
    }
}
