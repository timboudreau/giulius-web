package com.mastfrog.statsd.aop;

/**
 * A counter. Bind these by calling StatsdModule.registerCounter(name), and then
 * ask for them to be injected using &#064;Named.
 *
 * @author Tim Boudreau
 */
public interface Counter {

    /**
     * The name of this counter
     *
     * @return its name
     */
    public String name();

    /**
     * Increment this counter
     *
     * @return this
     */
    public Counter increment();

    /**
     * Decremen this counter
     *
     * @return this
     */
    public Counter decrement();
}
