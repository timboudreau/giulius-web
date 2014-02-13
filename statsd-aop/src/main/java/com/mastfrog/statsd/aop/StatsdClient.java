package com.mastfrog.statsd.aop;

import com.google.inject.ImplementedBy;
import com.mastfrog.util.thread.QuietAutoCloseable;

/**
 * A Statsd client - really a wrapper for one.
 *
 * @author Tim Boudreau
 */
@ImplementedBy(MockStatsdClient.class)
public interface StatsdClient {

    /**
     * Time some code - the close() method of the returned QuietAutoCloseable
     * records the timing, so it can be used in a try-with-resources block.
     *
     * @param string The name of the metric
     * @return An AutoCloseable which does not throw an exception in its close()
     * method and which automatically records the timing
     */
    QuietAutoCloseable benchmark(final String string);

    /**
     * Set a counter
     *
     * @param string The name of the counter
     * @param value The value
     * @return this
     */
    StatsdClient count(String string, int value);

    /**
     * Decrement a counter
     *
     * @param string The name of the counter
     * @return this
     */
    StatsdClient decrement(String string);

    /**
     * Set a gauge
     *
     * @param string the name of the gauge
     * @param value the value
     * @return this
     */
    StatsdClient gauge(String string, int value);

    /**
     * Increment a counter
     *
     * @param name The name of the counter
     * @return this
     */
    StatsdClient increment(String name);

    /**
     * Record a timing
     *
     * @param timing the name of the timing
     * @param millis the value in milliseconds
     * @return this
     */
    StatsdClient time(String timing, int millis);

    /**
     * Get a named counter - typically, you ask Guice to inject these, using the
     * &#064;Named annnotation
     *
     * @param name The name of the counter
     * @return this
     */
    Counter counter(String name);
}
