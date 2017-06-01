package com.mastfrog.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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
    private final List<Class<? extends JacksonConfigurer>> declarativeConfigurers
             = new LinkedList<>();
    private final String bindingName;

    /**
     * Create a new JacksonModule which will <i>not</i> load from
     * meta-inf/services, but only bind the configurers passed here
     * and any passed later to <code>withConfigurer</code>.
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

    /**
     * Create a JacksonModule which will <i>not</i> look in ServiceLoader
     * but only bind those configurers passed to it.
     * 
     * @param configurers The configurers
     */
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

    /**
     * Create a new JacksonModule which will use ServiceLoader to find instances
     * of JacksonConfigurer on the classpath, and bind them using 
     * &#064;Named and thhe passed binding name
     * 
     * @param bindingName The binding name to make the object mapper available
     * under
     */
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
    
    public JacksonModule withJavaTimeSerializationMode(TimeSerializationMode timeMode, DurationSerializationMode durationMode) {
        if (timeMode == null) {
            throw new IllegalArgumentException("Null mode");
        }
        for (Iterator<JacksonConfigurer> iter = configurers.iterator(); iter.hasNext();) {
            if (iter.next() instanceof JavaTimeConfigurer) {
                iter.remove();
                break;
            }
        }
        configurers.add(new JavaTimeConfigurer(timeMode, durationMode));
        return this;
    }

    private static JacksonConfigurer[] loadFromMetaInfServices() {
        List<JacksonConfigurer> all = new ArrayList<>(10);
        for (JacksonConfigurer c : ServiceLoader.load(JacksonConfigurer.class)) {
            all.add(c);
        }
        return all.toArray(new JacksonConfigurer[all.size()]);
    }

    public JacksonModule withConfigurer(JacksonConfigurer configurer) {
        if (configurer == null) {
            throw new NullPointerException("configurer");
        }
        this.configurers.add(configurer);
        return this;
    }
    
    public JacksonModule withConfigurer(Class<? extends JacksonConfigurer> type) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        if (!JacksonConfigurer.class.isAssignableFrom(type)) {
            throw new ClassCastException(type.getName() + " is not a subtype of " + JacksonConfigurer.class.getName());
        }
        this.declarativeConfigurers.add(type);
        return this;
    }

    @Override
    protected void configure() {
        List<Provider<? extends JacksonConfigurer>> configurers = new LinkedList<>();
        for (Class<? extends JacksonConfigurer> type : declarativeConfigurers) {
            Provider<? extends JacksonConfigurer> p = binder().getProvider(type);
            configurers.add(p);
        }
        if (bindingName != null) {
            bind(ObjectMapper.class).annotatedWith(Names.named(bindingName))
                    .toProvider(new JacksonProvider(configurers));
        } else {
            bind(ObjectMapper.class).toProvider(new JacksonProvider(configurers));
        }
    }

    @Singleton
    private class JacksonProvider implements Provider<ObjectMapper> {

        private ObjectMapper mapper = new ObjectMapper();
        private final AtomicBoolean configured = new AtomicBoolean();
        private final List<Provider<? extends JacksonConfigurer>> providers;
        
        JacksonProvider(List<Provider<? extends JacksonConfigurer>> providers) {
            this.providers = providers;
        }

        @Override
        public ObjectMapper get() {
            if (configured.compareAndSet(false, true)) {
                for (JacksonConfigurer config : configurers) {
                    mapper = config.configure(mapper);
                }
                for (Provider<? extends JacksonConfigurer> provider : providers) {
                    mapper = provider.get().configure(mapper);
                }
            }
            return mapper;
        }
    }
}
