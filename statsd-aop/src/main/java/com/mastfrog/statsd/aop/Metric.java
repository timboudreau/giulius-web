package com.mastfrog.statsd.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate methods with this to get automatic stats generation and publication
 * if you use StatsdModule.
 *
 * @author Tim Boudreau
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface Metric {

    /**
     * The name of the metric
     * @return the name
     */
    String value();

    /**
     * The type of metric
     * @return the type
     * 
     */
    Types type();

    public enum Types {

        /**
         * Increment the named value on method entry
         */
        INCREMENT,
        /**
         * Decrement the named value on method entry
         */
        DECREMENT,
        /**
         * Time the annotated method
         */
        TIME,
        /**
         * Update the number of threads concurrently in the annotated
         * method on method entry and exit
         */
        CONCURRENCY
    }
}
