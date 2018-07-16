package com.mastfrog.statsd.aop;

import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matcher;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;
import com.mastfrog.giulius.Dependencies;
import com.mastfrog.giulius.ShutdownHookRegistry;
import com.mastfrog.giulius.annotations.Defaults;
import com.mastfrog.settings.Settings;
import static com.mastfrog.statsd.aop.StatsdModule.SETTINGS_KEY_STATSD_HOST;
import static com.mastfrog.statsd.aop.StatsdModule.SETTINGS_KEY_STATSD_PORT;
import static com.mastfrog.statsd.aop.StatsdModule.SETTINGS_KEY_STATSD_PREFIX;
import static com.mastfrog.statsd.aop.StatsdModule.SETTINGS_KEY_STATSD_TIME_TO_LIVE;
import com.mastfrog.util.preconditions.ConfigurationError;
import java.lang.reflect.AnnotatedElement;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import com.mastfrog.util.thread.QuietAutoCloseable;

/**
 * Provides some Guice/AOP goodness to the standard Statsd client.
 * <p>
 * <b>Important</b>: If you do not set the setting <code>statsd.enabled</code>
 * in the Settings passed to the constructor, you will get a mock implementation
 * - that is the default behavior.
 * <p>
 * Also set <code>statsd.prefix</code>, <code>statsd.host</code> and
 * <code>statsd.port</code> to choose the name and where to send data.
 *
 * @author Tim Boudreau
 */
@Defaults(SETTINGS_KEY_STATSD_PORT + "=49601\n" + SETTINGS_KEY_STATSD_HOST + "=localhost\n"
        + SETTINGS_KEY_STATSD_PREFIX + "=noname\n" + SETTINGS_KEY_STATSD_TIME_TO_LIVE + "=-1\n")
public class StatsdModule extends AbstractModule implements StatsdConfig<StatsdModule> {

    public static final String SETTINGS_KEY_STATSD_PREFIX = "statsd.prefix";
    public static final String SETTINGS_KEY_STATSD_HOST = "statsd.host";
    public static final String SETTINGS_KEY_STATSD_PORT = "statsd.port";
    public static final String SETTINGS_KEY_STATSD_ENABLED = "statsd.enabled";
    public static final String SETTINGS_KEY_STATSD_TIME_TO_LIVE = "statsd.ttl";

    public static final String SETTINGS_KEY_PERIODIC_INTERVAL_SECONDS = "statsd.periodic.interval.seconds";
    public static final int DEFAULT_PERIODIC_INTERVAL_SECONDS = 240;

    private final Settings settings;
    private final Set<String> counters = new HashSet<>();
    private final Class<? extends StatsdClient> clientType;

    private final Set<Class<? extends Periodic>> periodics = new HashSet<>();

    /**
     * Construct a StatsdModule, using the provided Settings object to supply
     * prefix, port, host, etc. Settings can be a wrapper for Properties files
     * or any other sort of key/value data.
     *
     * @param settings A settings
     */
    public StatsdModule(Settings settings) {
        this(settings, null);
    }

    /**
     * Protected constructor for tests
     *
     * @param settings The settings
     * @param clientType The actual type to bind, or null
     */
    protected StatsdModule(Settings settings, Class<? extends StatsdClient> clientType) { // for tests
        this.settings = settings;
        this.clientType = clientType;
    }

    /**
     * Register the name of a counter, so you can &#064Inject <code>
     * &#064;Named("theName") Counter counter</code>
     *
     * @param name The counter name
     * @return this
     */
    public final StatsdModule registerCounter(String name) {
        counters.add(name);
        return this;
    }

    /**
     * Registers a periodic gauge (which will be instantiated by Guice and can
     * use &#064;Inject) which is called at an interval to set a statsd gauge
     * (i.e. periodically count the number of users, requests, widgets,
     * whatever).
     *
     * @param type The type
     * @return this
     */
    public final StatsdModule registerPeriodic(Class<? extends Periodic> type) {
        if (!Periodic.class.isAssignableFrom(type)) {
            throw new ClassCastException("Not a subclass of " + Periodic.class.getName() + ": " + type);
        }
        if (type.isLocalClass()) {
            throw new ClassCastException(type + " cannot be instantited by Guice - it is not static");
        }
        periodics.add(type);
        return this;
    }

    /**
     * In case someone subclasses, use this in place of configure()
     */
    protected void onConfigure() {

    }

    @Override
    protected final void configure() {
        String name = settings.getString(SETTINGS_KEY_STATSD_PREFIX);
        boolean enabled = settings.getBoolean(SETTINGS_KEY_STATSD_ENABLED, false);
        boolean production = Dependencies.isProductionMode(settings);
        if (production && enabled && "noname".equals(name)) {
            throw new ConfigurationError(SETTINGS_KEY_STATSD_PREFIX + " is not "
                    + "set.  Will not run this way in production mode.");
        }
        if (clientType != null && enabled) {
            bind(StatsdClient.class).to(clientType).in(Scopes.SINGLETON);
        } else {
            if (enabled) {
                System.out.println("Statsd enabled.");
                bind(StatsdClient.class).to(StatsdClientImpl.class).asEagerSingleton();
            } else {
                System.err.println("Stats not enabled - using mock statsd client");
            }
        }
        for (String counterName : counters) {
            bind(Counter.class).annotatedWith(Names.named(counterName)).toProvider(new CounterProvider(counterName, binder().getProvider(StatsdClient.class))).in(Scopes.SINGLETON);
        }
        if (enabled) {
            Matcher<AnnotatedElement> m = Matchers.annotatedWith(Metric.class);
            binder().bindInterceptor(Matchers.any(), m, new MetricInterceptor(binder().getProvider(StatsdClient.class)));
            onConfigure();
            if (enabled && !periodics.isEmpty()) {
                bind(new TL()).toInstance(periodics);
                bind(PeriodicsStarter.class).asEagerSingleton();
            }
        }
    }

    private static class PeriodicsStarter implements Runnable {

        private final Timer timer = new java.util.Timer("statsd.periodic", true);

        @Inject
        @SuppressWarnings("LeakingThisInConstructor")
        PeriodicsStarter(Set<Class<? extends Periodic>> types, Dependencies deps, StatsdClient client, Settings settings, ShutdownHookRegistry reg) {
            reg.add(this);
            // Get out of our own way here
            timer.schedule(new LaunchTask(types, deps, client, settings), 750);
        }

        private final class LaunchTask extends TimerTask {

            private final Set<Class<? extends Periodic>> types;
            private final Dependencies deps;
            private final StatsdClient client;
            private final Settings settings;

            LaunchTask(Set<Class<? extends Periodic>> types, Dependencies deps, StatsdClient client, Settings settings) {
                this.types = types;
                this.deps = deps;
                this.client = client;
                this.settings = settings;
            }

            @Override
            public void run() {
                Duration period = Duration.ofSeconds(settings.getInt(SETTINGS_KEY_PERIODIC_INTERVAL_SECONDS, DEFAULT_PERIODIC_INTERVAL_SECONDS));
                for (Class<? extends Periodic> type : types) {
                    Periodic p = deps.getInstance(type);
                    TimerTask task = p.start(client);
                    Duration d = p.interval(period);
                    long millis = d.toMillis();
                    timer.scheduleAtFixedRate(task, millis, millis);
                }
            }
        }

        @Override
        public void run() {
            timer.cancel();
        }
    }

    static class TL extends TypeLiteral<java.util.Set<Class<? extends Periodic>>> {

    }

    private static class CounterProvider implements Provider<Counter> {

        private final String name;
        private Counter counter;
        private final Provider<StatsdClient> client;

        public CounterProvider(String name, Provider<StatsdClient> client) {
            this.name = name;
            this.client = client;
        }

        @Override
        public Counter get() {
            if (counter == null) {
                synchronized (this) {
                    if (counter == null) {
                        counter = client.get().counter(name);
                    }
                }
            }
            return counter;
        }
    }

    private static class MetricInterceptor implements MethodInterceptor {

        private final Provider<StatsdClient> clientProvider;
        private final Map<String, AtomicInteger> concurrency
                = Maps.newConcurrentMap();

        public MetricInterceptor(Provider<StatsdClient> client) {
            this.clientProvider = client;
        }

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            Metric metric = invocation.getMethod().getAnnotation(Metric.class);
            StatsdClient client = clientProvider.get();
            switch (metric.type()) {
                case INCREMENT:
                    client.increment(metric.value());
                    return invocation.proceed();
                case DECREMENT:
                    client.decrement(metric.value());
                    return invocation.proceed();
                case TIME:
                    try (QuietAutoCloseable c = client.benchmark(metric.value())) {
                        return invocation.proceed();
                    }
                case CONCURRENCY:
                    AtomicInteger threadCount = concurrency.get(metric.value());
                    if (threadCount == null) {
                        threadCount = new AtomicInteger();
                        concurrency.put(metric.value(), threadCount);
                    }
                    int count = threadCount.incrementAndGet();
                    try {
                        return invocation.proceed();
                    } finally {
                        threadCount.decrementAndGet();
                        client.count(metric.value(), count);
                    }
                default:
                    throw new AssertionError();
            }
        }
    }
}
