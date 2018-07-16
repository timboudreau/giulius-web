package com.mastfrog.statsd.aop;

import com.mastfrog.util.thread.QuietAutoCloseable;

/**
 * Mock implementation
 *
 * @author Tim Boudreau
 */
class MockStatsdClient implements StatsdClient, QuietAutoCloseable {

    @Override
    public QuietAutoCloseable benchmark(String string) {
        return this;
    }

    @Override
    public StatsdClient count(String string, int i) {
        return this;
    }

    @Override
    public StatsdClient decrement(String string) {
        return this;
    }

    @Override
    public StatsdClient gauge(String string, int i) {
        return this;
    }

    @Override
    public StatsdClient increment(String string) {
        return this;
    }

    @Override
    public StatsdClient time(String string, int i) {
        return this;
    }

    @Override
    public void close() {
        // do nothing
    }

    @Override
    public Counter counter(String name) {
        return new CounterImpl(name);
    }

    static class CounterImpl implements Counter {

        private final String name;

        public CounterImpl(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Counter increment() {
            return this;
        }

        @Override
        public Counter decrement() {
            return this;
        }
    }
}
