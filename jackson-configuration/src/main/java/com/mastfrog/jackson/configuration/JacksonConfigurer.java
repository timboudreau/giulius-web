/*
 * The MIT License
 *
 * Copyright 2022 Mastfrog Technologies.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.mastfrog.jackson.configuration;

import com.mastfrog.jackson.configuration.impl.JavaTimeConfigurer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mastfrog.jackson.configuration.impl.LocaleJacksonConfigurer;
import com.mastfrog.jackson.configuration.impl.JavaOptionalSerializer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Implement this interface and register it using &#064;ServiceProvider to allow
 * it to contribute configuration to Jackson on initialization, or if using
 * JacksonModule with guice, pass it or its type to one of the methods there.
 *
 * @author Tim Boudreau
 */
public interface JacksonConfigurer extends Comparable<JacksonConfigurer> {

    /**
     * Configure the passed object mapper, possibly adding modules or otherwise
     * altering its configuration. Note that ObjectMapper is stateful because it
     * can be configured on the fly - if you are writing something that hands
     * out ObjectMappers using dependency injector or similar, which is
     * configured with some JacksonConfigurers, always use <code>copy()</code>
     * to return a configured instance so the code you call cannot interfere
     * with other uses of ObjectMapper in the application.
     *
     * @param m The object mapper
     * @return The same object mapper, ideally
     */
    ObjectMapper configure(ObjectMapper m);

    /**
     * Arbitrary ordering for to ensure configurers are applied in a particular
     * order in the case of a need to override one with another. The default is
     * 0, which is used by all built-in configurers.
     *
     * @return an int
     */
    default int precedence() {
        return 0;
    }

    /**
     * By default, returns the simple name of the JacksonConfigurer. Do not
     * override unless you are writing a JacksonConfigurer that wrappers another
     * one somehow - this is used to de-duplicate the list of configurers
     * configured in JacksonModule, to ensure there is only one configuration
     * for a given set of types present.
     *
     * @return A name
     */
    default String name() {
        return getClass().getSimpleName();
    }

    /**
     * Implements comparison by precedence.
     *
     * @param o Another configurer
     * @return an int
     */
    @Override
    default int compareTo(JacksonConfigurer o) {
        return Integer.compare(precedence(), o.precedence());
    }

    /**
     * Get a configurer that handles java.time types using ISO 8601 strings. The
     * following types are handled:
     * <ul>
     * <li>Instant</li>
     * <li>ZonedDateTime</li>
     * <li>LocalDateTime</li>
     * <li>OffsetDateTime</li>
     * <li>Duration</li>
     * <li>Period</li>
     * </ul>
     *
     * @return A configurer
     */
    public static JacksonConfigurer javaTimeConfigurer() {
        return new JavaTimeConfigurer(TimeSerializationMode.TIME_AS_ISO_STRING,
                DurationSerializationMode.DURATION_AS_ISO_STRING);
    }

    /**
     * Get a configurer that handles java.time classes, using the passed
     * serialization strategies. The following types are handled:
     * <ul>
     * <li>Instant</li>
     * <li>ZonedDateTime</li>
     * <li>LocalDateTime</li>
     * <li>OffsetDateTime</li>
     * <li>Duration</li>
     * <li>Period</li>
     * </ul>
     *
     * @param timeMode The time mode
     * @param durationMode The duration mode
     * @return A configurer
     */
    public static JacksonConfigurer javaTimeConfigurer(TimeSerializationMode timeMode,
            DurationSerializationMode durationMode) {
        return new JavaTimeConfigurer(timeMode, durationMode);
    }

    /**
     * Get a configurer that reads and writes java Locales as their
     * standards-based codes rather than Java constant names, for
     * interoperability with other languages that expect standards-based entries
     * for locales.
     *
     * @return A configurer
     */
    public static JacksonConfigurer localeConfigurer() {
        return new LocaleJacksonConfigurer();
    }

    /**
     * Get a serializer-only configurer that writes Java Optionals as either
     * null or their value.
     *
     * @return A configurer
     */
    public static JacksonConfigurer optionalSerializer() {
        return new JavaOptionalSerializer();
    }

    /**
     * Get all of the configurers registered via META-INF/services via
     * ServiceLoader.
     *
     * @return A collection of JacksonConfigurers.
     */
    public static Collection<? extends JacksonConfigurer> metaInfServices() {
        List<JacksonConfigurer> all = new ArrayList<>(10);
        for (JacksonConfigurer c : ServiceLoader.load(JacksonConfigurer.class)) {
            all.add(c);
        }
        return all;
    }

    /**
     * Configure an ObjectMapper using all configurers in META-INF/services.
     *
     * @param orig A mapper
     * @return A mapper
     */
    public static ObjectMapper configureFromMetaInfServices(ObjectMapper orig) {
        return apply(orig, metaInfServices());
    }

    /**
     * Convenience method to apply a bunch of configurers to an ObjectMapper.
     *
     * @param mapper A mapper
     * @param configs some configurers
     * @return the object mapper
     */
    public static ObjectMapper apply(ObjectMapper mapper, Iterable<? extends JacksonConfigurer> configs) {
        for (JacksonConfigurer cf : configs) {
            mapper = cf.configure(mapper);
        }
        return mapper;
    }

    /**
     * Convenience method to apply a bunch of configurers to an ObjectMapper.
     *
     * @param mapper A mapper
     * @param configs some configurers
     * @return the object mapper
     */
    public static ObjectMapper apply(ObjectMapper mapper, JacksonConfigurer... configs) {
        for (JacksonConfigurer cf : configs) {
            mapper = cf.configure(mapper);
        }
        return mapper;
    }

}
