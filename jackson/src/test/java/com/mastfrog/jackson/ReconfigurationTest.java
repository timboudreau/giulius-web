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
package com.mastfrog.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.mastfrog.jackson.JacksonModule.JC;
import com.mastfrog.jackson.JacksonModule.JacksonProvider;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author timb
 */
public class ReconfigurationTest {

    private static final long MILLIS = 1496043237000L;

    JacksonModule.JacksonProvider provider(Supplier<JacksonModule> c) {
        JacksonModule m = c.get();
        Injector inj = Guice.createInjector(m);
        JC mapperProvider = inj.getInstance(JC.class);

        assertTrue("Not a JacksonProvider.  Proxied? "
                + mapperProvider.getClass().getName(),
                mapperProvider instanceof JacksonModule.JacksonProvider);

        return ((JacksonModule.JacksonProvider) mapperProvider);
    }

    @Test
    public void testOnlyOneOfType() throws JsonProcessingException {
        JacksonModule.JacksonProvider prov = provider(()
                -> new JacksonModule(true));
        assertNoDuplicates(prov);

        ObjectMapper om = prov.get().enable(SerializationFeature.INDENT_OUTPUT);
        TimeThing tt = new TimeThing();
        String val = om.writeValueAsString(tt);
        TimeThing nue = om.readValue(val, TimeThing.class);

        assertEquals(tt, nue);
        assertEquals(tt.toString(), nue.toString());

        assertTrue(names(prov).contains("LocaleJacksonConfigurer"));

        LocaleThing loc = new LocaleThing();

        String ls = om.writeValueAsString(loc);

        LocaleThing des = om.readValue(ls, LocaleThing.class);
        assertEquals(loc, des);
        
        assertTrue(ls.contains("en-US"));
        assertTrue(ls.contains("en-GB"));
        assertTrue(ls.contains("zh-TW"));
    }

    @Test
    public void testHttpTimes() throws Exception {
        JacksonModule.JacksonProvider prov = provider(()
                -> new JacksonModule(true)
                        .withJavaTimeSerializationMode(com.mastfrog.jackson.configuration.TimeSerializationMode.TIME_AS_EPOCH_MILLIS, com.mastfrog.jackson.configuration.DurationSerializationMode.DURATION_AS_MILLIS)
                        .withJavaTimeSerializationMode(com.mastfrog.jackson.configuration.TimeSerializationMode.HTTP_HEADER_FORMAT, com.mastfrog.jackson.configuration.DurationSerializationMode.DURATION_AS_STRING)
        );
        ObjectMapper om = prov.get().enable(SerializationFeature.INDENT_OUTPUT);
        TimeThing tt = new TimeThing();
        String val = om.writeValueAsString(tt);
        TimeThing nue = om.readValue(val, TimeThing.class);

        assertTrue(val.contains("Mon, 29 May 2017 07:33:57 Z"));

        assertEquals(tt, nue);
        assertEquals(Instant.ofEpochMilli(MILLIS), nue.instant);
    }

    @Test
    public void testNoMetaInfShouldBeEmpty() {
        JacksonModule.JacksonProvider prov = provider(()
                -> new JacksonModule(false));
        List<Class<?>> list = classList(prov);
        assertNoDuplicates(prov);
        assertEquals(0, list.size());
    }

    @Test
    public void testReconfiguringTime() {
        JacksonModule.JacksonProvider prov = provider(()
                -> new JacksonModule(true)
                        .withJavaTimeSerializationMode(com.mastfrog.jackson.configuration.TimeSerializationMode.TIME_AS_EPOCH_MILLIS, com.mastfrog.jackson.configuration.DurationSerializationMode.DURATION_AS_MILLIS)
                        .withJavaTimeSerializationMode(com.mastfrog.jackson.configuration.TimeSerializationMode.HTTP_HEADER_FORMAT, com.mastfrog.jackson.configuration.DurationSerializationMode.DURATION_AS_STRING)
        );
        assertNoDuplicates(prov);
    }

    private void assertNoDuplicates(JacksonProvider prov) {
        List<Class<?>> types = classList(prov);
        assertEquals("Type set contains duplicates: " + types,
                new HashSet<>(types).size(), types.size());

        List<String> names = names(prov);
        assertEquals("Name set contains duplicates: " + names,
                new HashSet<>(names).size(), names.size());
    }

    @SuppressWarnings("deprecation")
    static List<String> names(JacksonModule.JacksonProvider prov) {
        List<String> result = new ArrayList<>();
        prov.configurers().forEach(cfig -> result.add(cfig.name()));
        return result;
    }

    @SuppressWarnings("deprecation")
    static List<Class<?>> classList(JacksonModule.JacksonProvider prov) {
        List<Class<?>> result = new ArrayList<>();
        List<com.mastfrog.jackson.JacksonConfigurer> configs = prov.configurers();
        for (com.mastfrog.jackson.JacksonConfigurer jc : configs) {
            if (jc instanceof WrapperJacksonConfigurer) {
                result.add(((WrapperJacksonConfigurer) jc).origType());
            } else {
                result.add(jc.getClass());
            }
        }
        return result;
    }

    static class LocaleThing {

        public final Locale a;
        public final Locale b;
        public final Locale c;

        @JsonCreator
        public LocaleThing(
                @JsonProperty("a") Locale a,
                @JsonProperty("b") Locale b,
                @JsonProperty("c") Locale c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        public LocaleThing() {
            this(Locale.US, Locale.UK, Locale.TAIWAN);
        }

        @Override
        public String toString() {
            return a.toLanguageTag() + " / " + b.toLanguageTag() + " / " + c.toLanguageTag();
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o == null || o.getClass() != LocaleThing.class) {
                return false;
            }
            LocaleThing other = (LocaleThing) o;

            return a.toLanguageTag().equals(other.a.toLanguageTag())
                    && b.toLanguageTag().equals(other.b.toLanguageTag())
                    && c.toLanguageTag().equals(other.c.toLanguageTag());

        }

        @Override
        public int hashCode() {
            return toString().hashCode() * 29;
        }
    }

    static class TimeThing {

        static final ZoneId GMT = ZoneId.of("GMT");
        public final OffsetDateTime offset;
        public final Instant instant;
        public final ZonedDateTime zoned;
        public final LocalDateTime local;

        @JsonCreator
        public TimeThing(
                @JsonProperty("offset") OffsetDateTime offset,
                @JsonProperty("instant") Instant instant,
                @JsonProperty("zoned") ZonedDateTime zoned,
                @JsonProperty("local") LocalDateTime loc) {
            this.offset = offset;
            this.instant = instant;
            this.zoned = zoned;
            this.local = loc;
        }

        public TimeThing(Instant instant) {
            this(OffsetDateTime.ofInstant(instant, GMT),
                    instant,
                    ZonedDateTime.ofInstant(instant, GMT),
                    LocalDateTime.ofInstant(instant, GMT));
        }

        public TimeThing() {
            this(Instant.ofEpochMilli(MILLIS));
        }

        @Override
        public String toString() {
            return "TimeThing{" + "offset=" + offset + ", instant=" + instant + ", zoned=" + zoned + ", local=" + local + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o == null || o.getClass() != TimeThing.class) {
                return false;
            }
            TimeThing tt = (TimeThing) o;
            return tt.instant.toEpochMilli() == instant.toEpochMilli();
        }

        @Override
        public int hashCode() {
            long val = instant.toEpochMilli();
            return (int) (val ^ (val >>> 32));
        }

    }
}
