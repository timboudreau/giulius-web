/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mastfrog.statsd.aop;

import com.mastfrog.statsd.aop.StatsdModule;
import com.mastfrog.statsd.aop.StatsdClient;
import com.mastfrog.statsd.aop.Counter;
import com.mastfrog.statsd.aop.Metric;
import com.google.common.collect.Maps;
import com.mastfrog.giulius.tests.GuiceRunner;
import com.mastfrog.giulius.tests.TestWith;
import com.mastfrog.settings.Settings;
import com.mastfrog.util.thread.QuietAutoCloseable;
import com.mastfrog.statsd.aop.StatsdClientTest.M;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Tim Boudreau
 */
@RunWith(GuiceRunner.class)
@TestWith(M.class)
public class StatsdClientTest {

    @Test
    public void test(StatsdClient client, Fixture fixture) throws InterruptedException {
        assertTrue(client instanceof StatsdClientImpl);
        StatsdClientImpl c = (StatsdClientImpl) client;
        fixture.moreThings();
        fixture.moreThings();
        assertEquals(2, c.value("things"));
        fixture.fewerThings();
        assertEquals(1, c.value("things"));

        fixture.doStuff();
        fixture.doStuff();
        fixture.doStuff();
        assertEquals(3, c.value("x"));
        fixture.doneWithStuff();
        assertEquals(2, c.value("x"));
        fixture.doneWithStuff();
        assertEquals(1, c.value("x"));

        fixture.waitAWhile();
        fixture.waitAWhile();
        int waits = c.value("waits");
        assertTrue(waits + " was too short", waits >= 200);
        assertEquals(0, c.value("widgets"));
        fixture.widgets.decrement();
        assertEquals(-1, c.value("widgets"));
    }

    static class Fixture {

        private final Counter widgets;
        private final Counter things;

        @Inject
        Fixture(@Named("widgets") Counter widgets, @Named("things") Counter things) {
            this.widgets = widgets;
            this.things = things;
        }

        public void moreThings() {
            things.increment();
        }

        public void fewerThings() {
            things.decrement();
        }

        @Metric(value = "x", type = Metric.Types.INCREMENT)
        public void doStuff() {
        }

        @Metric(value = "x", type = Metric.Types.DECREMENT)
        public void doneWithStuff() {
        }

        @Metric(value = "waits", type = Metric.Types.TIME)
        public void waitAWhile() throws InterruptedException {
            Thread.sleep(200);
        }
    }

    static class M extends StatsdModule {

        public M(Settings settings) {
            super(settings, StatsdClientImpl.class);
            registerCounter("widgets");
            registerCounter("things");
        }

    }

    @Singleton
    public static class StatsdClientImpl implements StatsdClient {

        final Map<String, AtomicInteger> stats = Maps.newConcurrentMap();

        static class NamedAI extends AtomicInteger {

            private final String name;

            public NamedAI(String name) {
                this.name = name;
            }

            @Override
            public String toString() {
                return name + " = " + super.toString();
            }
        }

        private AtomicInteger get(String name) {
            AtomicInteger in = stats.get(name);
            if (in == null) {
                in = new NamedAI(name);
                stats.put(name, in);
            }
            return in;
        }

        public int value(String name) {
            AtomicInteger in = get(name);
            return in == null ? -1 : in.get();
        }

        public QuietAutoCloseable benchmark(String string) {
            return new Timer(string, this);
        }

        public StatsdClient count(String string, int i) {
            get(string).set(i);
            return this;
        }

        public StatsdClient decrement(String string) {
            get(string).decrementAndGet();
            return this;
        }

        public StatsdClient gauge(String string, int i) {
            get(string).set(i);
            return this;
        }

        public StatsdClient increment(String string) {
            get(string).incrementAndGet();
            return this;
        }

        public StatsdClient stop() {
            return this;
        }

        public StatsdClient time(String string, int i) {
            get(string).set(i);
            return this;
        }

        public Counter counter(String name) {
            return new CounterImpl(name);
        }

        private class CounterImpl implements Counter {

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
                StatsdClientImpl.this.increment(name);
                return this;
            }

            @Override
            public Counter decrement() {
                StatsdClientImpl.this.decrement(name);
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
}
