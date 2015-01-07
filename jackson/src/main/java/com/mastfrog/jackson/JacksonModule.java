package com.mastfrog.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * A module which binds Jackson's ObjectMapper, and allows it to be configured
 * by multiple JacksonConfigurers looked up on the classpath using
 * Lookup/ServiceLoader
 *
 * @author Tim Boudreau
 */
public final class JacksonModule extends AbstractModule {

    private final JacksonConfigurer[] configurers;

    /**
     * Create a new JacksonModule which will <i>not</i> load from
     * meta-inf/services.
     *
     * @param configurers An explicit list of jackson configurers
     */
    public JacksonModule(JacksonConfigurer... configurers) {
        for (JacksonConfigurer c : configurers) {
            if (c == null) {
                throw new IllegalArgumentException("Null configurer");
            }
        }
        this.configurers = configurers;
    }

    /**
     * Create a new JacksonModule which will use ServiceLoader to find instances
     * of JacksonConfigurer on the classpath to use.
     */
    public JacksonModule() {
        this(loadFromMetaInfServices());
    }

    private static JacksonConfigurer[] loadFromMetaInfServices() {
        List<JacksonConfigurer> all = new ArrayList<>(10);
        for (JacksonConfigurer c : ServiceLoader.load(JacksonConfigurer.class)) {
            all.add(c);
        }
        return all.toArray(new JacksonConfigurer[all.size()]);
    }

    @Override
    protected void configure() {
        bind(ObjectMapper.class).toProvider(new JacksonProvider());
    }

    @Singleton
    private class JacksonProvider implements Provider<ObjectMapper> {

        private volatile ObjectMapper mapper;

        @Override
        public ObjectMapper get() {
            if (mapper == null) {
                synchronized (this) {
                    if (mapper == null) {
                        mapper = new ObjectMapper();
                        for (JacksonConfigurer config : configurers) {
                            mapper = config.configure(mapper);
                        }
                    }
                }
            }
            return mapper;
        }
    }
}
