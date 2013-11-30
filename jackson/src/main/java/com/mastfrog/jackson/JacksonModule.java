package com.mastfrog.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.openide.util.Lookup;

/**
 * A module which binds Jackson's ObjectMapper, and allows it to be configured
 * by multiple JacksonConfigurers looked up on the classpath using
 * Lookup/ServiceLoader
 *
 * @author Tim Boudreau
 */
public final class JacksonModule extends AbstractModule {

    private final JacksonConfigurer[] configurers;

    public JacksonModule(JacksonConfigurer... configurers) {
        this.configurers = configurers;
    }

    public JacksonModule() {
        configurers = Lookup.getDefault().lookupAll(JacksonConfigurer.class).toArray(new JacksonConfigurer[0]);
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
