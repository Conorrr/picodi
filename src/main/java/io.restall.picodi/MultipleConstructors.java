package io.restall.picodi;

/**
 * Thrown when a picodi attempts to instantiate a class but it has multiple constructors.
 */
public class MultipleConstructors extends RuntimeException {

    public MultipleConstructors(String message, Object... formatArgs) {
        super(String.format(message, formatArgs));
    }
}
