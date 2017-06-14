/*
 * The MIT License
 *
 * Copyright 2017 Tim Boudreau.
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.mastfrog.giulius.tests.GuiceRunner;
import com.mastfrog.giulius.tests.TestWith;
import com.mastfrog.jackson.JacksonModuleTest.IsoStringTimeSerializationConfigModule;
import com.mastfrog.jackson.JacksonModuleTest.MillisTimeSerializationConfigModule;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import static com.mastfrog.jackson.TimeSerializationMode.TIME_AS_EPOCH_MILLIS;
import java.time.Duration;
import java.time.Period;

/**
 *
 * @author Tim Boudreau
 */
@RunWith(GuiceRunner.class)
@TestWith(iterate = {MillisTimeSerializationConfigModule.class, IsoStringTimeSerializationConfigModule.class})
public class JacksonModuleTest {

    private static final Instant WHEN = Instant.ofEpochMilli(1496247701503L);
    private static final Instant LATER = WHEN.plus(Duration.ofHours(2).plus(Duration.ofMinutes(3)).plus(Duration.ofSeconds(5)).plus(Duration.ofMillis(854)));
    private static final ZoneId ZONE = ZoneId.of("America/New_York");

    static class MillisTimeSerializationConfigModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(TimeSerializationMode.class).toInstance(TIME_AS_EPOCH_MILLIS);
            bind(DurationSerializationMode.class).toInstance(DurationSerializationMode.DURATION_AS_MILLIS);
            install(new JacksonModule()
                    .withJavaTimeSerializationMode(TimeSerializationMode.TIME_AS_EPOCH_MILLIS,
                            DurationSerializationMode.DURATION_AS_MILLIS));
        }
    }

    static class IsoStringTimeSerializationConfigModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(TimeSerializationMode.class).toInstance(TimeSerializationMode.TIME_AS_ISO_STRING);
            bind(DurationSerializationMode.class).toInstance(DurationSerializationMode.DURATION_AS_STRING);
            install(new JacksonModule()
                    .withJavaTimeSerializationMode(TimeSerializationMode.TIME_AS_ISO_STRING,
                            DurationSerializationMode.DURATION_AS_STRING));
        }
    }

    @Test
    public void testSerializationAndDeserialization(ObjectMapper m, TimeSerializationMode timeMode, DurationSerializationMode durationMode) throws JsonProcessingException, IOException {
        System.out.println("\n\n*********************\n\n" + timeMode + "\t" + durationMode + "\n\n");
        ZonedDateTime zdt = ZonedDateTime.ofInstant(WHEN, ZONE);
        LocalDateTime ldt = LocalDateTime.ofInstant(WHEN, ZONE);
        OffsetDateTime odt = OffsetDateTime.ofInstant(WHEN, ZONE).withOffsetSameInstant(ZoneOffset.UTC);
        ZoneOffset offset = ZONE.getRules().getOffset(WHEN);
        Duration dur = Duration.between(WHEN, LATER);
        Period per = Period.of(5, 7, 23);
        

        assertEquals(WHEN, testOne(ZonedDateTime.class, zdt.withZoneSameInstant(ZoneId.systemDefault()), m).toInstant());
        assertEquals(WHEN, testOne(LocalDateTime.class, ldt, m).toInstant(ZONE.getRules().getOffset(WHEN)));
        if (timeMode == TimeSerializationMode.TIME_AS_ISO_STRING) {
            assertEquals(WHEN, testOne(OffsetDateTime.class, odt, m).toInstant());
        }
        assertEquals(WHEN, testOne(Instant.class, WHEN, m));
        assertEquals(ZONE, testOne(ZoneId.class, ZONE, m));
        assertEquals(ZONE.getRules().getOffset(WHEN), testOne(ZoneOffset.class, offset, m));
        assertEquals(dur, testOne(Duration.class, dur, m));
        assertEquals(per, testOne(Period.class, per, m));
    }

    private <T> T testOne(Class<T> valueType, T value, ObjectMapper m) throws JsonProcessingException, IOException {
        String serialized = m.writeValueAsString(value);
        System.out.println(valueType.getSimpleName() + " -> '" + serialized + "'  = " + value);
        T read = m.readValue(serialized, valueType);
        assertTrue(valueType.isInstance(value));
        assertEquals("Read value  of " + valueType.getSimpleName()
                + " not equal: " + serialized, value, read);

        return read;
    }

}
