package com.mastfrog.statsd.aop;

import com.mastfrog.giulius.ShutdownHookRegistry;
import com.mastfrog.util.thread.QuietAutoCloseable;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import javax.inject.Named;

/**
 * Implements a real statsd client
 *
 * @author Tim Boudreau
 */
final class StatsdClientImpl implements StatsdClient, Runnable {

    private final StatsDClient statsd;

    StatsdClientImpl(@Named(StatsdModule.SETTINGS_KEY_STATSD_HOST) String host, @Named(StatsdModule.SETTINGS_KEY_STATSD_PORT) int port, @Named(StatsdModule.SETTINGS_KEY_STATSD_PREFIX) String prefix, ShutdownHookRegistry reg) {
        statsd = new NonBlockingStatsDClient(prefix, host, port);
        reg.add(this);
    }

    @Override
    public StatsdClient count(String string, int i) {
        statsd.count(string, i);
        return this;
    }

    @Override
    public StatsdClient increment(String string) {
        statsd.increment(string);
        return this;
    }

    @Override
    public StatsdClient decrement(String string) {
        statsd.decrement(string);
        return this;
    }

    @Override
    public StatsdClient gauge(String string, int i) {
        statsd.gauge(string, i);
        return this;
    }

    @Override
    public StatsdClient time(String string, int i) {
        statsd.time(string, i);
        return this;
    }

    @Override
    public QuietAutoCloseable benchmark(final String string) {
        return new Timer(string, this);
    }

    @Override
    public void run() {
        statsd.stop();
    }

    @Override
    public Counter counter(String name) {
        return new CounterImpl(name, this);
    }

    private static class CounterImpl implements Counter {

        private final String name;
        private final StatsdClientImpl client;

        CounterImpl(String name, StatsdClientImpl client) {
            this.name = name;
            this.client = client;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Counter increment() {
            client.increment(name);
            return this;
        }

        @Override
        public Counter decrement() {
            client.decrement(name);
            return this;
        }
    }

    private static final class Timer extends QuietAutoCloseable {

        private final String name;
        private final long now = System.currentTimeMillis();
        private final StatsdClientImpl impl;

        Timer(String name, StatsdClientImpl impl) {
            this.name = name;
            this.impl = impl;
        }

        @Override
        public void close() {
            int elapsedNanos = (int) (System.currentTimeMillis() - now);
            impl.time(name, elapsedNanos);
        }
    }
}
