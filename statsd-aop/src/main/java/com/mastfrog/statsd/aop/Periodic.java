package com.mastfrog.statsd.aop;

import com.mastfrog.util.preconditions.Checks;
import java.time.Duration;
import java.util.TimerTask;

/**
 * A thing which updates a statsd gauge and is called periodically on a timer.
 *
 * @author Tim Boudreau
 */
public abstract class Periodic {

    private final String name;

    private final Duration interval;

    /**
     * Create a periodic gauge with this name
     *
     * @param name A name
     */
    protected Periodic(String name) {
        this(name, null);
    }

    /**
     * Create with a custom interval
     *
     * @param name The gauge name
     * @param interval An interval
     */
    protected Periodic(String name, Duration interval) {
        Checks.notNull("name", name);
        this.name = name;
        this.interval = interval;
    }

    /**
     * Get the current value
     *
     * @return a value
     */
    protected abstract int get();

    TimerTask start(StatsdClient client) {
        TaskImpl result = new TaskImpl(client);
        return result;
    }

    Duration interval(Duration defaultInterval) {
        return interval == null ? defaultInterval : interval;
    }

    private class TaskImpl extends TimerTask {

        private final StatsdClient client;

        public TaskImpl(StatsdClient client) {
            this.client = client;
        }

        @Override
        public void run() {
            client.gauge(name, get());
        }
    }
}
