package com.mastfrog.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A module which binds Jackson's ObjectMapper, and allows it to be configured
 * by multiple JacksonConfigurers looked up on the classpath using
 * Lookup/ServiceLoader
 *
 * @author Tim Boudreau
 */
public final class JacksonModule extends AbstractModule {

    private final List<JacksonConfigurer> configurers;
    private final String bindingName;

    /**
     * Create a new JacksonModule which will <i>not</i> load from
     * meta-inf/services.
     *
     * @param configurers An explicit list of jackson configurers
     */
    public JacksonModule(String bindingName, JacksonConfigurer... configurers) {
        this.bindingName = bindingName;
        for (JacksonConfigurer c : configurers) {
            if (c == null) {
                throw new IllegalArgumentException("Null configurer");
            }
        }
        this.configurers = new LinkedList<>(Arrays.asList(configurers));
    }

    public JacksonModule(JacksonConfigurer... configurers) {
        for (JacksonConfigurer c : configurers) {
            if (c == null) {
                throw new IllegalArgumentException("Null configurer");
            }
        }
        this.configurers = new LinkedList<>(Arrays.asList(configurers));
        this.bindingName = null;
    }

    /**
     * Create a new JacksonModule which will use ServiceLoader to find instances
     * of JacksonConfigurer on the classpath to use.
     */
    public JacksonModule() {
        this((String) null);
    }

    public JacksonModule(String bindingName) {
        this (bindingName, true);
    }

    public JacksonModule(String bindingName, boolean loadFromMetaInfServices) {
        this.bindingName = bindingName;
        if (!loadFromMetaInfServices) {
            this.configurers = new LinkedList<>();
        } else {
            this.configurers = new LinkedList<>(Arrays.asList(loadFromMetaInfServices()));
        }
    }

    private static JacksonConfigurer[] loadFromMetaInfServices() {
        List<JacksonConfigurer> all = new ArrayList<>(10);
        for (JacksonConfigurer c : ServiceLoader.load(JacksonConfigurer.class)) {
            all.add(c);
        }
        return all.toArray(new JacksonConfigurer[all.size()]);
    }

    public JacksonModule withConfigurer(JacksonConfigurer configurer) {
        this.configurers.add(configurer);
        return this;
    }

    @Override
    protected void configure() {
        if (bindingName != null) {
            bind(ObjectMapper.class).annotatedWith(Names.named(bindingName))
                    .toProvider(new JacksonProvider());
            System.out.println("BIND JACKSON NAME " + bindingName);
        } else {
            bind(ObjectMapper.class).toProvider(new JacksonProvider());
        }
    }

    @Singleton
    private class JacksonProvider implements Provider<ObjectMapper> {

        private ObjectMapper mapper = new ObjectMapper();
        private final AtomicBoolean configured = new AtomicBoolean();

        @Override
        public ObjectMapper get() {
            if (configured.compareAndSet(false, true)) {
                System.out.println("CONIGURE JACKSON WITH:");
                for (JacksonConfigurer config : configurers) {
                    System.out.println(config);
                    mapper = config.configure(mapper);
                }
            }
            return mapper;
        }
    }
}
