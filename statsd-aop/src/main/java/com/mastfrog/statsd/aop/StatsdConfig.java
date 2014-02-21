package com.mastfrog.statsd.aop;

/**
 *
 * @author Tim Boudreau
 */
public interface StatsdConfig<T extends StatsdConfig> {

    T registerPeriodic(Class<? extends Periodic> type);

    T registerCounter(String name);
}
