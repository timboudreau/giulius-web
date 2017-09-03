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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mastfrog.util.time.TimeUtil;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service = JacksonConfigurer.class)
public class JavaTimeConfigurer implements JacksonConfigurer {

    private final TimeSerializationMode timeMode;
    private final DurationSerializationMode durationMode;

    public JavaTimeConfigurer() {
        this(TimeSerializationMode.TIME_AS_EPOCH_MILLIS, DurationSerializationMode.DURATION_AS_MILLIS);
    }

    public JavaTimeConfigurer(TimeSerializationMode mode, DurationSerializationMode durationMode) {
        this.timeMode = mode;
        this.durationMode = durationMode;
    }

    @Override
    public ObjectMapper configure(ObjectMapper mapper) {
        SimpleModule sm = new SimpleModule("java-time", new Version(1, 0, 0, null, "com.mastfrog", "java-time"));
        sm.addSerializer(ZoneOffset.class, new ZoneOffsetSerializer());
        sm.addDeserializer(ZoneOffset.class, new ZoneOffsetDeserializer());
        sm.addSerializer(ZoneId.class, new ZoneIdSerializer());
        sm.addDeserializer(ZoneId.class, new ZoneIdDeserializer());

        switch (durationMode) {
            case DURATION_AS_MILLIS:
                sm.addSerializer(new DurationToLongSerializer());
                sm.addDeserializer(Duration.class, new DurationFromLongDeserializer());
                sm.addSerializer(Period.class, new CompactPeriodSerializer());
                sm.addDeserializer(Period.class, new CompactPeriodDeserializer());
                break;
            case DURATION_AS_STRING:
                sm.addSerializer(new DurationToStringSerializer());
                sm.addDeserializer(Duration.class, new DurationFromStringDeserializer());
                sm.addSerializer(Period.class, new PeriodSerializer());
                sm.addDeserializer(Period.class, new PeriodDeserializer());
                break;
            default:
                throw new AssertionError(durationMode);
        }

        switch (timeMode) {
            case TIME_AS_EPOCH_MILLIS:
                sm.addSerializer(ZonedDateTime.class, new ZonedDateTimeToLongSerializer());
                sm.addDeserializer(ZonedDateTime.class, new ZonedDateTimeFromLongDeserializer());
                sm.addSerializer(OffsetDateTime.class, new OffsetDateTimeToLongSerializer());
                sm.addDeserializer(OffsetDateTime.class, new OffsetDateTimeToLongDeserializer());
                sm.addSerializer(LocalDateTime.class, new LocalDateTimeToLongSerializer());
                sm.addDeserializer(LocalDateTime.class, new LocalDateTimeFromLongDeserializer());
                sm.addSerializer(Instant.class, new InstantToLongSerializer());
                sm.addDeserializer(Instant.class, new InstantFromLongDeserializer());
                break;
            case TIME_AS_ISO_STRING:
                sm.addSerializer(ZonedDateTime.class, new ZonedDateTimeToIsoStringSerializer());
                sm.addDeserializer(ZonedDateTime.class, new ZonedDateTimeToIsoStringDeserializer());
                sm.addSerializer(OffsetDateTime.class, new OffsetDateTimeToIsoStringSerializer());
                sm.addDeserializer(OffsetDateTime.class, new OffsetDateTimeToIsoStringDeserializer());
                sm.addSerializer(LocalDateTime.class, new LocalDateTimeToIsoStringSerializer());
                sm.addDeserializer(LocalDateTime.class, new LocalDateTimeToIsoStringDeserializer());
                sm.addSerializer(Instant.class, new InstantToIsoStringSerializer());
                sm.addDeserializer(Instant.class, new InstantToIsoStringDeserializer());
                break;
            default:
                throw new AssertionError(timeMode);
        }
        mapper.registerModule(sm);
        return mapper;
    }

    private static final class DurationToLongSerializer extends JsonSerializer<Duration> {

        @Override
        public Class<Duration> handledType() {
            return Duration.class;
        }

        @Override
        public void serialize(Duration t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            sp.defaultSerializeValue(t == null ? 0L : t.toMillis(), jg);
        }
    }

    private static final class DurationFromLongDeserializer extends JsonDeserializer<Duration> {

        @Override
        public Class<Duration> handledType() {
            return Duration.class;
        }

        @Override
        public Duration deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            long epochMillis = jp.readValueAs(Long.TYPE);
            return Duration.ofMillis(epochMillis);
        }
    }

    private static final class DurationToStringSerializer extends JsonSerializer<Duration> {

        @Override
        public Class<Duration> handledType() {
            return Duration.class;
        }

        @Override
        public void serialize(Duration t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            sp.defaultSerializeValue(TimeUtil.format(t), jg);
        }
    }

    private static final class DurationFromStringDeserializer extends JsonDeserializer<Duration> {

        @Override
        public Class<Duration> handledType() {
            return Duration.class;
        }

        @Override
        public Duration deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            String s = jp.readValueAs(String.class);
            return TimeUtil.parse(s);
        }
    }

    private static final class ZonedDateTimeToLongSerializer extends JsonSerializer<ZonedDateTime> {

        @Override
        public Class<ZonedDateTime> handledType() {
            return ZonedDateTime.class;
        }

        @Override
        public void serialize(ZonedDateTime t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            sp.defaultSerializeValue(t == null ? 0L : t.toInstant().toEpochMilli(), jg);
        }

    }

    private static final class ZonedDateTimeFromLongDeserializer extends JsonDeserializer<ZonedDateTime> {

        @Override
        public Class<?> handledType() {
            return ZonedDateTime.class;
        }

        @Override
        public ZonedDateTime deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            if (!jp.currentToken().isNumeric()) {
                String timestamp = jp.readValueAs(String.class);
                return ZonedDateTime.parse(timestamp, DateTimeFormatter.ISO_ZONED_DATE_TIME);
            }
            long epochMillis = jp.readValueAs(Long.TYPE);
            return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault());
        }
    }

    private static final class OffsetDateTimeToLongDeserializer extends JsonDeserializer<OffsetDateTime> {

        @Override
        public Class<?> handledType() {
            return OffsetDateTime.class;
        }

        @Override
        public OffsetDateTime deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            long epochMillis = jp.readValueAs(Long.TYPE);
            return Instant.ofEpochMilli(epochMillis).atOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
        }
    }

    private static final class OffsetDateTimeToLongSerializer extends JsonSerializer<OffsetDateTime> {

        @Override
        public Class<OffsetDateTime> handledType() {
            return OffsetDateTime.class;
        }

        @Override
        public void serialize(OffsetDateTime t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            sp.defaultSerializeValue(t == null ? 0L : t.toInstant().toEpochMilli(), jg);
        }
    }

    private static final class InstantFromLongDeserializer extends JsonDeserializer<Instant> {

        @Override
        public Class<?> handledType() {
            return Instant.class;
        }

        @Override
        public Instant deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            long epochMillis = jp.readValueAs(Long.TYPE);
            return Instant.ofEpochMilli(epochMillis);
        }
    }

    private static final class InstantToLongSerializer extends JsonSerializer<Instant> {

        @Override
        public Class<Instant> handledType() {
            return Instant.class;
        }

        @Override
        public void serialize(Instant t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            sp.defaultSerializeValue(t == null ? 0L : t.toEpochMilli(), jg);
        }
    }

    private static final class LocalDateTimeFromLongDeserializer extends JsonDeserializer<LocalDateTime> {

        @Override
        public Class<?> handledType() {
            return LocalDateTime.class;
        }

        @Override
        public LocalDateTime deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            long epochMillis = jp.readValueAs(Long.TYPE);
            return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
    }

    private static final class LocalDateTimeToLongSerializer extends JsonSerializer<LocalDateTime> {

        @Override
        public Class<LocalDateTime> handledType() {
            return LocalDateTime.class;
        }

        @Override
        public void serialize(LocalDateTime t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            sp.defaultSerializeValue(t == null ? 0L : t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), jg);
        }
    }

    static final class ZoneIdDeserializer extends JsonDeserializer<ZoneId> {

        @Override
        public Class<?> handledType() {
            return ZoneId.class;
        }

        @Override
        public ZoneId deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            String id = jp.readValueAs(String.class);
            return ZoneId.of(id);
        }
    }

    static final class ZoneIdSerializer extends JsonSerializer<ZoneId> {

        @Override
        public Class<ZoneId> handledType() {
            return ZoneId.class;
        }

        @Override
        public void serialize(ZoneId t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            sp.defaultSerializeValue(t.getId(), jg);
        }
    }

    static final class ZoneOffsetDeserializer extends JsonDeserializer<ZoneOffset> {

        @Override
        public Class<?> handledType() {
            return ZoneOffset.class;
        }

        @Override
        public ZoneOffset deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            int offset = jp.readValueAs(Integer.TYPE);
            return ZoneOffset.ofTotalSeconds(offset);
        }
    }

    static final class ZoneOffsetSerializer extends JsonSerializer<ZoneOffset> {

        @Override
        public Class<ZoneOffset> handledType() {
            return ZoneOffset.class;
        }

        @Override
        public void serialize(ZoneOffset t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            sp.defaultSerializeValue(t.getTotalSeconds(), jg);
        }
    }

    static final class ZonedDateTimeToIsoStringSerializer extends JsonSerializer<ZonedDateTime> {

        @Override
        public Class<ZonedDateTime> handledType() {
            return ZonedDateTime.class;
        }

        @Override
        public void serialize(ZonedDateTime t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            String formatted = DateTimeFormatter.ISO_ZONED_DATE_TIME.format(t);
            sp.defaultSerializeValue(formatted, jg);
        }

    }

    static final class ZonedDateTimeToIsoStringDeserializer extends JsonDeserializer<ZonedDateTime> {

        @Override
        public Class<?> handledType() {
            return ZonedDateTime.class;
        }

        @Override
        public ZonedDateTime deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            if (jp.currentToken().isNumeric()) {
                long epochMillis = jp.readValueAs(Long.TYPE);
                return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault());
            }
            String timestamp = jp.readValueAs(String.class);
            return ZonedDateTime.parse(timestamp, DateTimeFormatter.ISO_ZONED_DATE_TIME);
        }
    }

    static final class OffsetDateTimeToIsoStringDeserializer extends JsonDeserializer<OffsetDateTime> {

        @Override
        public Class<?> handledType() {
            return OffsetDateTime.class;
        }

        @Override
        public OffsetDateTime deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            String timestamp = jp.readValueAs(String.class);
            return OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_ZONED_DATE_TIME);
        }
    }

    static final class OffsetDateTimeToIsoStringSerializer extends JsonSerializer<OffsetDateTime> {

        @Override
        public Class<OffsetDateTime> handledType() {
            return OffsetDateTime.class;
        }

        @Override
        public void serialize(OffsetDateTime t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            String formatted = DateTimeFormatter.ISO_ZONED_DATE_TIME.format(t);
            sp.defaultSerializeValue(formatted, jg);
        }
    }

    static final class InstantToIsoStringDeserializer extends JsonDeserializer<Instant> {

        @Override
        public Class<?> handledType() {
            return Instant.class;
        }

        @Override
        public Instant deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            String timestamp = jp.readValueAs(String.class);
            return Instant.parse(timestamp);
        }
    }

    static final class InstantToIsoStringSerializer extends JsonSerializer<Instant> {

        @Override
        public Class<Instant> handledType() {
            return Instant.class;
        }

        @Override
        public void serialize(Instant t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            sp.defaultSerializeValue(t.toString(), jg);
        }
    }

    static final class LocalDateTimeToIsoStringDeserializer extends JsonDeserializer<LocalDateTime> {

        @Override
        public Class<?> handledType() {
            return LocalDateTime.class;
        }

        @Override
        public LocalDateTime deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            String timestamp = jp.readValueAs(String.class);
            return LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }

    static final class LocalDateTimeToIsoStringSerializer extends JsonSerializer<LocalDateTime> {

        @Override
        public Class<LocalDateTime> handledType() {
            return LocalDateTime.class;
        }

        @Override
        public void serialize(LocalDateTime t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            String fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(t);
            sp.defaultSerializeValue(fmt, jg);
        }
    }

    static final class PeriodSerializer extends JsonSerializer<Period> {

        @Override
        public Class<Period> handledType() {
            return Period.class;
        }

        @Override
        public void serialize(Period t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            jg.writeStartObject();
            try {
                for (TemporalUnit unit : t.getUnits()) {
                    jg.writeObjectField(unit.toString().toLowerCase(), t.get(unit));
                }
            } finally {
                jg.writeEndObject();
            }
        }
    }

    static final class PeriodDeserializer extends JsonDeserializer<Period> {

        @Override
        public Class<?> handledType() {
            return Period.class;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Period deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            Map<String, Object> m = jp.readValueAs(Map.class);
            Period p = Period.ZERO;
            for (Map.Entry<String, Object> e : m.entrySet()) {
                ChronoUnit unit = ChronoUnit.valueOf(e.getKey().toUpperCase());
                long val = ((Number) e.getValue()).longValue();
                switch (unit) {
                    case DAYS:
                        p = p.plusDays(val);
                        break;
                    case MONTHS:
                        p = p.plusMonths(val);
                        break;
                    case YEARS:
                        p = p.plusYears(val);
                        break;
                    default:
                        throw new AssertionError(unit);
                }
            }
            return p;
        }
    }

    static final class CompactPeriodSerializer extends JsonSerializer<Period> {

        @Override
        public Class<Period> handledType() {
            return Period.class;
        }

        @Override
        public void serialize(Period t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            String val = new StringBuilder(20).append('P')
                    .append(t.getYears()).append('Y')
                    .append(t.getMonths()).append('M')
                    .append(t.getDays()).append('D').toString();
            sp.defaultSerializeValue(val, jg);
        }
    }

    static final class CompactPeriodDeserializer extends JsonDeserializer<Period> {

        private static final Pattern PAT = Pattern.compile("P(\\d+)Y(\\d+)M(\\d+)D");

        @Override
        public Class<?> handledType() {
            return Period.class;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Period deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            String s = jp.readValueAs(String.class);
            Matcher m = PAT.matcher(s);
            if (m.find()) {
                int years = Integer.parseInt(m.group(1));
                int months = Integer.parseInt(m.group(2));
                int days = Integer.parseInt(m.group(3));
                return Period.of(years, months, days);
            } else {
                throw new IOException("Period not in format PnYnMnD: '" + s + "'");
            }

        }
    }

}
