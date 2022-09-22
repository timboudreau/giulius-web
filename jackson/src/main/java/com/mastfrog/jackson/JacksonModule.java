package com.mastfrog.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import static com.mastfrog.jackson.WrapperJacksonConfigurer.wrap;
import static com.mastfrog.util.preconditions.Checks.notNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A module which binds Jackson's ObjectMapper, and allows it to be configured
 * by multiple JacksonConfigurers looked up on the classpath using
 * Lookup/ServiceLoader
 *
 * @author Tim Boudreau
 */
public final class JacksonModule extends AbstractModule {

    @SuppressWarnings("deprecation")
    private final List<com.mastfrog.jackson.JacksonConfigurer> configurers;
    @SuppressWarnings("deprecation")
    private final List<Class<? extends com.mastfrog.jackson.JacksonConfigurer>> declarativeConfigurers
            = new LinkedList<>();
    private final List<Class<? extends com.mastfrog.jackson.configuration.JacksonConfigurer>> declarativeConfigurers2
            = new LinkedList<>();
    private final String bindingName;

    /**
     * Create a new JacksonModule which will <i>not</i> load from
     * meta-inf/services, but only bind the configurers passed here and any
     * passed later to <code>withConfigurer</code>.
     *
     * @param configurers An explicit list of jackson configurers
     */
    @SuppressWarnings("deprecation")
    public JacksonModule(String bindingName, com.mastfrog.jackson.JacksonConfigurer... configurers) {
        this.bindingName = bindingName;
        for (com.mastfrog.jackson.JacksonConfigurer c : configurers) {
            if (c == null) {
                throw new IllegalArgumentException("Null configurer");
            }
        }
        this.configurers = new LinkedList<>(Arrays.asList(configurers));
    }

    /**
     * Create a JacksonModule which will <i>not</i> look in ServiceLoader but
     * only bind those configurers passed to it.
     *
     * @param configurers The configurers
     */
    @SuppressWarnings("deprecation")
    public JacksonModule(com.mastfrog.jackson.JacksonConfigurer... configurers) {
        for (com.mastfrog.jackson.JacksonConfigurer c : configurers) {
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
     * of JacksonConfigurer on the classpath, and bind them using &#064;Named
     * and thhe passed binding name
     *
     * @param bindingName The binding name to make the object mapper available
     * under
     */
    public JacksonModule(String bindingName) {
        this(bindingName, true);
    }

    public JacksonModule(boolean loadFromMetaInfServices) {
        this(null, loadFromMetaInfServices);
    }

    public JacksonModule(String bindingName, boolean loadFromMetaInfServices) {
        this.bindingName = bindingName;
        if (!loadFromMetaInfServices) {
            this.configurers = new LinkedList<>();
        } else {
            this.configurers = new LinkedList<>(metaInfServices());
        }
    }

    /**
     * Add declarative meta-inf-services configurers from this library <i>and
     * the jackson-configuration</i> library, with the following caveat: If an
     * instance of the same type as one defined in meta-inf-services is already
     * present, do not replace the existing instance.
     * <p>
     * Where possible, prefer the constructor argument, which loads these ahead
     * of others.
     * </p>
     *
     * @return this
     */
    @SuppressWarnings("deprecation")
    public JacksonModule loadFromMetaInfServices() {
        Set<Class<?>> classes = new HashSet<>();
        classes.addAll(declarativeConfigurers);
        classes.addAll(declarativeConfigurers2);
        for (com.mastfrog.jackson.JacksonConfigurer jc : configurers) {
            if (jc instanceof WrapperJacksonConfigurer) {
                classes.add(((WrapperJacksonConfigurer) jc).origType());
            } else {
                classes.add(jc.getClass());
            }
            if (jc instanceof com.mastfrog.jackson.JavaTimeConfigurer) {
                classes.add(com.mastfrog.jackson.configuration.impl.JavaTimeConfigurer.class);
            }
        }
        for (com.mastfrog.jackson.configuration.JacksonConfigurer jc : com.mastfrog.jackson.configuration.JacksonConfigurer.metaInfServices()) {
            if (!classes.contains(jc.getClass())) {
                configurers.add(wrap(jc));
                classes.add(jc.getClass());
            }
        }
        for (com.mastfrog.jackson.JacksonConfigurer jc : metaInfServices()) {
            Class<?> type;
            if (jc instanceof WrapperJacksonConfigurer) {
                type = ((WrapperJacksonConfigurer) jc).origType();
            } else {
                type = jc.getClass();
            }
            if (!classes.contains(type)) {
                configurers.add(jc);
            }
        }
        return this;
    }

    @SuppressWarnings("deprecation")
    public JacksonModule withJavaTimeSerializationMode(com.mastfrog.jackson.configuration.TimeSerializationMode timeMode, com.mastfrog.jackson.configuration.DurationSerializationMode durationMode) {
        if (timeMode == null) {
            throw new IllegalArgumentException("Null time mode");
        }
        if (durationMode == null) {
            throw new IllegalArgumentException("Null duration mode");
        }
        for (Iterator<com.mastfrog.jackson.JacksonConfigurer> iter = configurers.iterator(); iter.hasNext();) {
            com.mastfrog.jackson.JacksonConfigurer cf = iter.next();
            if ("JavaTimeConfigurer".equals(cf.name())) {
                iter.remove();
            }
        }
        configurers.add(new com.mastfrog.jackson.JavaTimeConfigurer(com.mastfrog.jackson.TimeSerializationMode.forAlternate(timeMode),
                com.mastfrog.jackson.DurationSerializationMode.forAlternate(durationMode)));
        return this;
    }

    @SuppressWarnings("deprecation")
    public JacksonModule withJavaTimeSerializationMode(com.mastfrog.jackson.TimeSerializationMode timeMode, com.mastfrog.jackson.DurationSerializationMode durationMode) {
        if (timeMode == null) {
            throw new IllegalArgumentException("Null time mode");
        }
        if (durationMode == null) {
            throw new IllegalArgumentException("Null duration mode");
        }
        for (Iterator<com.mastfrog.jackson.JacksonConfigurer> iter = configurers.iterator(); iter.hasNext();) {
            com.mastfrog.jackson.JacksonConfigurer cf = iter.next();
            if ("JavaTimeConfigurer".equals(cf.name())) {
                iter.remove();
            }
        }
        configurers.add(new JavaTimeConfigurer(timeMode, durationMode));
        return this;
    }

    @SuppressWarnings("deprecation")
    private static Collection<? extends com.mastfrog.jackson.JacksonConfigurer> metaInfServices() {
        List<com.mastfrog.jackson.JacksonConfigurer> all = new ArrayList<>(10);
        for (com.mastfrog.jackson.JacksonConfigurer c : ServiceLoader.load(com.mastfrog.jackson.JacksonConfigurer.class)) {
            all.add(c);
        }
        for (com.mastfrog.jackson.configuration.JacksonConfigurer c : com.mastfrog.jackson.configuration.JacksonConfigurer.metaInfServices()) {
            all.add(wrap(c));
        }
        return all;
    }

    @SuppressWarnings("deprecation")
    public JacksonModule withConfigurer(com.mastfrog.jackson.JacksonConfigurer configurer) {
        prune(notNull("configurer", configurer).name());
        this.configurers.add(configurer);
        return this;
    }

    public JacksonModule withConfigurer(com.mastfrog.jackson.configuration.JacksonConfigurer configurer) {
        prune(notNull("configurer", configurer).name());
        this.configurers.add(wrap(configurer));
        return this;
    }

    @SuppressWarnings("deprecation")
    public JacksonModule withConfigurer(
            Class<? extends com.mastfrog.jackson.JacksonConfigurer> type) {
        if (!com.mastfrog.jackson.JacksonConfigurer.class.isAssignableFrom(notNull("type", type))) {
            throw new ClassCastException(type.getName() + " is not a subtype of " + JacksonConfigurer.class.getName());
        }
        prune(type.getSimpleName());
        this.declarativeConfigurers.add(type);
        return this;
    }

    @SuppressWarnings("deprecation")
    private void prune(String sn) {
        for (Iterator<Class<? extends com.mastfrog.jackson.JacksonConfigurer>> it = declarativeConfigurers.iterator(); it.hasNext();) {
            Class<? extends com.mastfrog.jackson.JacksonConfigurer> c = it.next();
            if (sn.equals(c.getSimpleName())) {
                it.remove();
            }
        }
        for (Iterator<Class<? extends com.mastfrog.jackson.configuration.JacksonConfigurer>> it = declarativeConfigurers2.iterator(); it.hasNext();) {
            Class<? extends com.mastfrog.jackson.configuration.JacksonConfigurer> c = it.next();
            if (sn.equals(c.getSimpleName())) {
                it.remove();
            }
        }
        for (Iterator<JacksonConfigurer> it = configurers.iterator(); it.hasNext();) {
            com.mastfrog.jackson.JacksonConfigurer cf = it.next();
            if (sn.equals(cf.name())) {
                it.remove();
            }
        }
    }

    public JacksonModule withConfigurer2(Class<? extends com.mastfrog.jackson.configuration.JacksonConfigurer> type) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        if (!com.mastfrog.jackson.configuration.JacksonConfigurer.class.isAssignableFrom(type)) {
            throw new ClassCastException(type.getName() + " is not a subtype of " + com.mastfrog.jackson.configuration.JacksonConfigurer.class.getName());
        }
        prune(type.getSimpleName());
        this.declarativeConfigurers2.add(type);
        return this;
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void configure() {
        List<Provider<? extends com.mastfrog.jackson.JacksonConfigurer>> configurers = new LinkedList<>();
        for (Class<? extends com.mastfrog.jackson.JacksonConfigurer> type : declarativeConfigurers) {
            Provider<? extends com.mastfrog.jackson.JacksonConfigurer> p
                    = binder().getProvider(type);
            configurers.add(p);
        }
        for (Class<? extends com.mastfrog.jackson.configuration.JacksonConfigurer> type : declarativeConfigurers2) {
            Provider<? extends com.mastfrog.jackson.JacksonConfigurer> p = WrapperProvider.wrapperProvider(binder().getProvider(type));
            configurers.add(p);
        }
        JacksonProvider prov = new JacksonProvider(configurers);
        if (bindingName != null) {
            bind(ObjectMapper.class)
                    .annotatedWith(Names.named(bindingName))
                    .toProvider(prov);
            // for tests
            bind(JC.class)
                    .annotatedWith(Names.named(bindingName))
                    .toInstance(prov);
        } else {
            bind(ObjectMapper.class).toProvider(prov);
            // for tests
            bind(JC.class).toInstance(prov);
        }
    }

    @SuppressWarnings("deprecation")
    private static class WrapperProvider<T extends com.mastfrog.jackson.configuration.JacksonConfigurer> implements Provider<com.mastfrog.jackson.JacksonConfigurer> {

        private final Provider<T> provider;

        WrapperProvider(Provider<T> type) {
            this.provider = type;
        }

        static <T extends com.mastfrog.jackson.configuration.JacksonConfigurer>
                WrapperProvider<T> wrapperProvider(Provider<T> provider) {
            return new WrapperProvider<>(provider);
        }

        @Override
        @SuppressWarnings("deprecation")
        public com.mastfrog.jackson.JacksonConfigurer get() {
            return wrap(provider.get());
        }
    }

    @Singleton
    @SuppressWarnings("deprecation")
    class JacksonProvider implements Provider<ObjectMapper>, JC {

        private ObjectMapper mapper = new ObjectMapper();
        private final AtomicBoolean configured = new AtomicBoolean();
        private @SuppressWarnings("deprecation")
        final List<Provider<? extends com.mastfrog.jackson.JacksonConfigurer>> providers;

        @SuppressWarnings("deprecation")
        JacksonProvider(List<Provider<? extends com.mastfrog.jackson.JacksonConfigurer>> providers) {
            this.providers = providers;
        }

        public List<com.mastfrog.jackson.JacksonConfigurer> configurers() {
            List<com.mastfrog.jackson.JacksonConfigurer> result = new ArrayList<>(configurers);
            for (Provider<? extends com.mastfrog.jackson.JacksonConfigurer> provider : providers) {
                com.mastfrog.jackson.JacksonConfigurer p = provider.get();
                result.add(p);
            }
            return result;
        }

        @Override
        @SuppressWarnings("deprecation")
        public ObjectMapper get() {
            if (configured.compareAndSet(false, true)) {
                for (com.mastfrog.jackson.JacksonConfigurer config : configurers()) {
                    mapper = config.configure(mapper);
                }
            }
            return mapper.copy();
        }
    }

    // for tests, we need a way to grab the concrete JacksonProvider instance,
    // which the injector will wrap
    interface JC {

        @SuppressWarnings("deprecation")
        List<com.mastfrog.jackson.JacksonConfigurer> configurers();

        public ObjectMapper get();
    }
}
