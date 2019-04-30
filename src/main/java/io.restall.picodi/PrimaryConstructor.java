package io.restall.picodi;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Indicates to Picodi which constructor to use if multiple are defined.
 */
@Retention(RUNTIME)
@Target(CONSTRUCTOR)
public @interface PrimaryConstructor {
}
