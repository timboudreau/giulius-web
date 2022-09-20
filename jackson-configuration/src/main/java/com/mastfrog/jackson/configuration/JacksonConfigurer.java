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
import com.mastfrog.jackson.configuration.impl.OptionalSerializer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Implement this interface and register it using &#064;ServiceProvider to allow
 * it to contribute configuration to Jackson on initialization.
 *
 * @author Tim Boudreau
 */
public interface JacksonConfigurer {

    /**
     * Configure the passed object mapper
     *
     * @param m The object mapper
     * @return An object mapper
     */
    ObjectMapper configure(ObjectMapper m);

    default String name() {
        return getClass().getSimpleName();
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
        return new OptionalSerializer();
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

    public static ObjectMapper apply(ObjectMapper mapper, Iterable<? extends JacksonConfigurer> configs) {
        for (JacksonConfigurer cf : configs) {
            mapper = cf.configure(mapper);
        }
        return mapper;
    }

    public static ObjectMapper apply(ObjectMapper mapper, JacksonConfigurer... configs) {
        for (JacksonConfigurer cf : configs) {
            mapper = cf.configure(mapper);
        }
        return mapper;
    }

}
