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
package com.mastfrog.jackson.configuration.impl;

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
import com.mastfrog.jackson.configuration.DurationSerializationMode;
import com.mastfrog.jackson.configuration.JacksonConfigurer;
import com.mastfrog.jackson.configuration.TimeSerializationMode;
import com.mastfrog.util.preconditions.Checks;
import com.mastfrog.util.service.ServiceProvider;
import com.mastfrog.util.strings.Strings;
import com.mastfrog.util.time.TimeUtil;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configures java.time classes. Note, this class is public because it is
 * registered in META-INF/services. This package will be hidden once this
 * library begins using the Java Module System.
 *
 * @author Tim Boudreau
 */
@ServiceProvider(JacksonConfigurer.class)
public class JavaTimeConfigurer implements JacksonConfigurer {

    private final TimeSerializationMode timeMode;
    private final DurationSerializationMode durationMode;
    static final DateTimeFormatter ISO_INSTANT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendInstant()
            .toFormatter(Locale.US);
    private static final ZoneId GMT = ZoneId.of("GMT");
    private final String stringVal; // for logging/debugging

    public JavaTimeConfigurer() {
        this(TimeSerializationMode.TIME_AS_ISO_STRING, DurationSerializationMode.DURATION_AS_ISO_STRING);
    }

    public JavaTimeConfigurer(TimeSerializationMode mode, DurationSerializationMode durationMode) {
        this.timeMode = mode;
        this.durationMode = durationMode;
        stringVal = "JavaTimeConfigurer(" + mode + "," + durationMode + ")";
    }

    @Override
    public String toString() {
        return stringVal;
    }

    @Override
    public ObjectMapper configure(ObjectMapper mapper) {
        SimpleModule sm = new SimpleModule("java-time", new Version(1, 0, 1, null, "com.mastfrog", "java-time"));
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
            case DURATION_AS_ISO_STRING:
                sm.addSerializer(new DurationToIsoStringSerializer());
                sm.addDeserializer(Duration.class, new DurationFromIsoStringDeserializer());
                sm.addSerializer(Period.class, new PeriodSerializer());
                sm.addDeserializer(Period.class, new PeriodDeserializer());
                break;
            case NONE:
                // do nothing
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
                sm.addSerializer(Date.class, new DateToLongSerializer());
                sm.addDeserializer(Date.class, new DateFromLongDeserializer());
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
                sm.addSerializer(Date.class, new DateToIsoStringSerializer());
                sm.addDeserializer(Date.class, new DateFromIsoStringDeserializer());
                break;
            case HTTP_HEADER_FORMAT:
                sm.addSerializer(ZonedDateTime.class, new ZonedDateTimeToHttpHeaderStringSerializer());
                sm.addDeserializer(ZonedDateTime.class, new ZonedDateTimeToHttpHeaderStringDeserializer());
                sm.addSerializer(OffsetDateTime.class, new OffsetDateTimeToHttpHeaderStringSerializer());
                sm.addDeserializer(OffsetDateTime.class, new OffsetDateTimeToHttpHeaderStringDeserializer());
                sm.addSerializer(LocalDateTime.class, new LocalDateTimeToHttpHeaderStringSerializer());
                sm.addDeserializer(LocalDateTime.class, new LocalDateTimeToHttpHeaderStringDeserializer());
                sm.addSerializer(Instant.class, new InstantToHttpHeaderStringSerializer());
                sm.addDeserializer(Instant.class, new InstantToHttpHeaderStringDeserializer());
                sm.addSerializer(Date.class, new DateToHttpHeaderStringSerializer());
                sm.addDeserializer(Date.class, new DateFromHttpHeaderStringDeserializer());
                break;
            case NONE:
                // do nothing
                break;
            default:
                throw new AssertionError(timeMode);
        }
        sm.addSerializer(MonthDay.class, new MonthDayToIsoStringSerializer());
        sm.addDeserializer(MonthDay.class, new MonthDayFromIsoStringDeserializer());
        sm.addSerializer(LocalDate.class, new LocalDateToIsoStringSerializer());
        sm.addDeserializer(LocalDate.class, new LocalDateFromIsoStringDeserializer());
        sm.addSerializer(LocalTime.class, new LocalTimeToIsoStringSerializer());
        sm.addDeserializer(LocalTime.class, new LocalTimeFromIsoStringDeserializer());
        sm.addSerializer(YearMonth.class, new YearMonthToIsoStringSerializer());
        sm.addDeserializer(YearMonth.class, new YearMonthFromIsoStringDeserializer());
        sm.addSerializer(Year.class, new YearToIsoStringSerializer());
        sm.addDeserializer(Year.class, new YearFromIsoStringDeserializer());
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

    private static final class DurationToIsoStringSerializer extends JsonSerializer<Duration> {

        @Override
        public Class<Duration> handledType() {
            return Duration.class;
        }

        @Override
        public void serialize(Duration t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            sp.defaultSerializeValue(t.toString(), jg);
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
            return TimeUtil.parseDuration(s);
        }
    }

    private static final class DurationFromIsoStringDeserializer extends JsonDeserializer<Duration> {

        @Override
        public Class<Duration> handledType() {
            return Duration.class;
        }

        @Override
        public Duration deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            String s = jp.readValueAs(String.class);
            return Duration.parse(s);
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
                return ZonedDateTime.parse(timestamp, ISO_INSTANT);
            }
            long epochMillis = jp.readValueAs(Long.TYPE);
            return Instant.ofEpochMilli(epochMillis).atZone(GMT);
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

    private static final class ZoneIdDeserializer extends JsonDeserializer<ZoneId> {

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

    private static final class ZoneIdSerializer extends JsonSerializer<ZoneId> {

        @Override
        public Class<ZoneId> handledType() {
            return ZoneId.class;
        }

        @Override
        public void serialize(ZoneId t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            sp.defaultSerializeValue(t.getId(), jg);
        }
    }

    private static final class ZoneOffsetDeserializer extends JsonDeserializer<ZoneOffset> {

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

    private static final class ZoneOffsetSerializer extends JsonSerializer<ZoneOffset> {

        @Override
        public Class<ZoneOffset> handledType() {
            return ZoneOffset.class;
        }

        @Override
        public void serialize(ZoneOffset t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            sp.defaultSerializeValue(t.getTotalSeconds(), jg);
        }
    }

    private static final class DateToIsoStringSerializer extends JsonSerializer<Date> {

        @Override
        public Class<Date> handledType() {
            return Date.class;
        }

        @Override
        public void serialize(Date t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            String formatted = ISO_INSTANT.format(Instant.ofEpochMilli(t.getTime()));
            sp.defaultSerializeValue(formatted, jg);
        }
    }

    private static final class DateToLongSerializer extends JsonSerializer<Date> {

        @Override
        public Class<Date> handledType() {
            return Date.class;
        }

        @Override
        public void serialize(Date t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            sp.defaultSerializeValue(t.getTime(), jg);
        }
    }

    private static final class ZonedDateTimeToIsoStringSerializer extends JsonSerializer<ZonedDateTime> {

        @Override
        public Class<ZonedDateTime> handledType() {
            return ZonedDateTime.class;
        }

        @Override
        public void serialize(ZonedDateTime t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            String formatted = ISO_INSTANT.format(t);
            sp.defaultSerializeValue(formatted, jg);
        }

    }

    private static final class DateFromLongDeserializer extends JsonDeserializer<Date> {

        @Override
        public Class<?> handledType() {
            return Date.class;
        }

        @Override
        public Date deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            long epochMillis = jp.readValueAs(Long.TYPE);
            return new Date(epochMillis);
        }
    }

    private static final class DateFromIsoStringDeserializer extends JsonDeserializer<Date> {

        @Override
        public Class<?> handledType() {
            return Date.class;
        }

        @Override
        public Date deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            if (jp.currentToken().isNumeric()) {
                long epochMillis = jp.readValueAs(Long.TYPE);
                return new Date(epochMillis);
            }
            String timestamp = jp.readValueAs(String.class);
            Instant inst = Instant.parse(timestamp);
            return new Date(inst.toEpochMilli());
        }
    }

    private static final class ZonedDateTimeToIsoStringDeserializer extends JsonDeserializer<ZonedDateTime> {

        @Override
        public Class<?> handledType() {
            return ZonedDateTime.class;
        }

        @Override
        public ZonedDateTime deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            if (jp.currentToken().isNumeric()) {
                long epochMillis = jp.readValueAs(Long.TYPE);
                return Instant.ofEpochMilli(epochMillis).atZone(GMT);
            }
            String timestamp = jp.readValueAs(String.class);
            Instant inst = Instant.parse(timestamp);
            return ZonedDateTime.ofInstant(inst, GMT);
        }
    }

    private static final class OffsetDateTimeToIsoStringDeserializer extends JsonDeserializer<OffsetDateTime> {

        @Override
        public Class<?> handledType() {
            return OffsetDateTime.class;
        }

        @Override
        public OffsetDateTime deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            String timestamp = jp.readValueAs(String.class);
            return OffsetDateTime.parse(timestamp);
        }
    }

    private static final class OffsetDateTimeToIsoStringSerializer extends JsonSerializer<OffsetDateTime> {

        @Override
        public Class<OffsetDateTime> handledType() {
            return OffsetDateTime.class;
        }

        @Override
        public void serialize(OffsetDateTime t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            String formatted = ISO_INSTANT.format(t);
            sp.defaultSerializeValue(formatted, jg);
        }
    }

    private static final class InstantToIsoStringDeserializer extends JsonDeserializer<Instant> {

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

    private static final class InstantToHttpHeaderStringDeserializer extends JsonDeserializer<Instant> {

        @Override
        public Class<?> handledType() {
            return Instant.class;
        }

        @Override
        public Instant deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            String timestamp = jp.readValueAs(String.class);
            ZonedDateTime zdt = parseZonedDateTime(timestamp);
            return zdt.toInstant();
        }
    }

    private static final class DateFromHttpHeaderStringDeserializer extends JsonDeserializer<Date> {

        @Override
        public Class<?> handledType() {
            return Date.class;
        }

        @Override
        public Date deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            String timestamp = jp.readValueAs(String.class);
            ZonedDateTime zdt = parseZonedDateTime(timestamp);
            return new Date(zdt.toInstant().toEpochMilli());
        }
    }

    private static final class ZonedDateTimeToHttpHeaderStringDeserializer extends JsonDeserializer<ZonedDateTime> {

        @Override
        public Class<?> handledType() {
            return ZonedDateTime.class;
        }

        @Override
        public ZonedDateTime deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            String timestamp = jp.readValueAs(String.class);
            return parseZonedDateTime(timestamp);
        }
    }

    private static final class OffsetDateTimeToHttpHeaderStringDeserializer extends JsonDeserializer<OffsetDateTime> {

        @Override
        public Class<?> handledType() {
            return OffsetDateTime.class;
        }

        @Override
        public OffsetDateTime deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            String timestamp = jp.readValueAs(String.class);
            return parseZonedDateTime(timestamp).toOffsetDateTime();
        }
    }

    private static final class LocalDateTimeToHttpHeaderStringDeserializer extends JsonDeserializer<LocalDateTime> {

        @Override
        public Class<?> handledType() {
            return LocalDateTime.class;
        }

        @Override
        public LocalDateTime deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            String timestamp = jp.readValueAs(String.class);
            return parseZonedDateTime(timestamp).toLocalDateTime();
        }
    }

    private static final class InstantToIsoStringSerializer extends JsonSerializer<Instant> {

        @Override
        public Class<Instant> handledType() {
            return Instant.class;
        }

        @Override
        public void serialize(Instant t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            sp.defaultSerializeValue(t.toString(), jg);
        }
    }

    private static final class InstantToHttpHeaderStringSerializer extends JsonSerializer<Instant> {

        @Override
        public Class<Instant> handledType() {
            return Instant.class;
        }

        @Override
        public void serialize(Instant t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            ZonedDateTime zdt = ZonedDateTime.ofInstant(t, GMT);
            sp.defaultSerializeValue(zdt.format(ISO2822DateFormat), jg);
        }
    }

    private static final class DateToHttpHeaderStringSerializer extends JsonSerializer<Date> {

        @Override
        public Class<Date> handledType() {
            return Date.class;
        }

        @Override
        public void serialize(Date t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(t.getTime()), GMT);
            sp.defaultSerializeValue(zdt.format(ISO2822DateFormat), jg);
        }
    }

    private static final class ZonedDateTimeToHttpHeaderStringSerializer extends JsonSerializer<ZonedDateTime> {

        @Override
        public Class<ZonedDateTime> handledType() {
            return ZonedDateTime.class;
        }

        @Override
        public void serialize(ZonedDateTime t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            sp.defaultSerializeValue(t.format(ISO2822DateFormat), jg);
        }
    }

    private static final class OffsetDateTimeToHttpHeaderStringSerializer extends JsonSerializer<OffsetDateTime> {

        @Override
        public Class<OffsetDateTime> handledType() {
            return OffsetDateTime.class;
        }

        @Override
        public void serialize(OffsetDateTime t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            sp.defaultSerializeValue(t.format(ISO2822DateFormat), jg);
        }
    }

    private static final class LocalDateTimeToHttpHeaderStringSerializer extends JsonSerializer<LocalDateTime> {

        @Override
        public Class<LocalDateTime> handledType() {
            return LocalDateTime.class;
        }

        @Override
        public void serialize(LocalDateTime t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            ZonedDateTime zdt = t.atZone(ZoneId.systemDefault());
            sp.defaultSerializeValue(zdt.format(ISO2822DateFormat), jg);
        }
    }

    private static final class LocalDateTimeToIsoStringDeserializer extends JsonDeserializer<LocalDateTime> {

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

    private static final class LocalDateTimeToIsoStringSerializer extends JsonSerializer<LocalDateTime> {

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

    private static final class MonthDayFromIsoStringDeserializer extends JsonDeserializer<MonthDay> {

        @Override
        public Class<?> handledType() {
            return MonthDay.class;
        }

        @Override
        public MonthDay deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            String timestamp = jp.readValueAs(String.class);
            return MonthDay.parse(timestamp);
        }
    }

    private static final class MonthDayToIsoStringSerializer extends JsonSerializer<MonthDay> {

        @Override
        public Class<MonthDay> handledType() {
            return MonthDay.class;
        }

        @Override
        public void serialize(MonthDay t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            String fmt = t.toString();
            sp.defaultSerializeValue(fmt, jg);
        }
    }

    private static final class LocalDateFromIsoStringDeserializer extends JsonDeserializer<LocalDate> {

        @Override
        public Class<?> handledType() {
            return LocalDate.class;
        }

        @Override
        public LocalDate deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            String timestamp = jp.readValueAs(String.class);
            return LocalDate.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE);
        }
    }

    private static final class LocalDateToIsoStringSerializer extends JsonSerializer<LocalDate> {

        @Override
        public Class<LocalDate> handledType() {
            return LocalDate.class;
        }

        @Override
        public void serialize(LocalDate t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            String fmt = t.format(DateTimeFormatter.ISO_LOCAL_DATE);
            sp.defaultSerializeValue(fmt, jg);
        }
    }

    private static final class LocalTimeFromIsoStringDeserializer extends JsonDeserializer<LocalTime> {

        @Override
        public Class<?> handledType() {
            return LocalTime.class;
        }

        @Override
        public LocalTime deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            String timestamp = jp.readValueAs(String.class);
            return LocalTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_TIME);
        }
    }

    private static final class LocalTimeToIsoStringSerializer extends JsonSerializer<LocalTime> {

        @Override
        public Class<LocalTime> handledType() {
            return LocalTime.class;
        }

        @Override
        public void serialize(LocalTime t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            String fmt = t.format(DateTimeFormatter.ISO_LOCAL_TIME);
            sp.defaultSerializeValue(fmt, jg);
        }
    }

    private static final class YearMonthFromIsoStringDeserializer extends JsonDeserializer<YearMonth> {

        @Override
        public Class<?> handledType() {
            return YearMonth.class;
        }

        @Override
        public YearMonth deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            String timestamp = jp.readValueAs(String.class);
            return YearMonth.parse(timestamp);
        }
    }

    private static final class YearMonthToIsoStringSerializer extends JsonSerializer<YearMonth> {

        @Override
        public Class<YearMonth> handledType() {
            return YearMonth.class;
        }

        @Override
        public void serialize(YearMonth t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            String fmt = t.toString();
            sp.defaultSerializeValue(fmt, jg);
        }
    }

    private static final class YearFromIsoStringDeserializer extends JsonDeserializer<Year> {

        @Override
        public Class<?> handledType() {
            return Year.class;
        }

        @Override
        public Year deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            String timestamp = jp.readValueAs(String.class);
            return Year.parse(timestamp);
        }
    }

    private static final class YearToIsoStringSerializer extends JsonSerializer<Year> {

        @Override
        public Class<Year> handledType() {
            return Year.class;
        }

        @Override
        public void serialize(Year t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            String fmt = t.toString();
            sp.defaultSerializeValue(fmt, jg);
        }
    }

    private static final class PeriodSerializer extends JsonSerializer<Period> {

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

    private static final class PeriodDeserializer extends JsonDeserializer<Period> {

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

    private static final class CompactPeriodSerializer extends JsonSerializer<Period> {

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

    private static final class CompactPeriodDeserializer extends JsonDeserializer<Period> {

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

    public static final DateTimeFormatter ISO2822DateFormat
            = new DateTimeFormatterBuilder()
                    .appendText(ChronoField.DAY_OF_WEEK, TextStyle.SHORT_STANDALONE).appendLiteral(", ")
                    .appendText(ChronoField.DAY_OF_MONTH, TextStyle.FULL).appendLiteral(" ")
                    .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT).appendLiteral(" ")
                    .appendText(ChronoField.YEAR, TextStyle.FULL).appendLiteral(" ")
                    .appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral(":")
                    .appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral(":")
                    .appendValue(ChronoField.SECOND_OF_MINUTE, 2).appendLiteral(" ")
                    .appendOffsetId().toFormatter();

    static final DateTimeFormatter TWO_DIGIT_YEAR
            = new DateTimeFormatterBuilder()
                    //                    .appendText(ChronoField.DAY_OF_WEEK, TextStyle.SHORT_STANDALONE).appendLiteral(", ")
                    .appendText(ChronoField.DAY_OF_MONTH, TextStyle.FULL).appendLiteral(" ")
                    .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT).appendLiteral(" ")
                    .appendValue(ChronoField.YEAR, 2, 4, SignStyle.NEVER).appendLiteral(" ")
                    .appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral(":")
                    .appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral(":")
                    .appendValue(ChronoField.SECOND_OF_MINUTE, 2).appendLiteral(" ")
                    .appendZoneOrOffsetId().toFormatter();

    @SuppressWarnings("deprecation")
    private static ZonedDateTime parseZonedDateTime(CharSequence value) {
        Checks.notNull("value", value);
        // Be permissive in what you accept, as they say
        long val;
        ZonedDateTime result;
        try {
            ZonedDateTime top = ZonedDateTime.parse(value, ISO2822DateFormat);
            result = mungeYear(top);
        } catch (DateTimeParseException e) {
            try {
                ZonedDateTime rfs = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME);
                result = mungeYear(rfs);
            } catch (DateTimeParseException e1) {
                e.addSuppressed(e1);
                try {
                    CharSequence munged = value;
                    int space = Strings.indexOf(' ', munged);
                    if (space != -1) {
                        munged = value.subSequence(space + 1, value.length());
                    }
                    ZonedDateTime dt = ZonedDateTime.parse(munged, TWO_DIGIT_YEAR);
                    result = mungeYear(dt);
                } catch (DateTimeParseException ex2) {
                    e.addSuppressed(ex2);
                    try {
                        //Sigh...use java.util.date to handle "GMT", "PST", "EST"
                        val = Date.parse(value.toString());
                        result = TimeUtil.fromUnixTimestamp(val);
                    } catch (IllegalArgumentException e3) {
                        e.addSuppressed(e3);
                        new IllegalArgumentException(value.toString(), e).printStackTrace(System.err);
                        return null;
                    }
                }
            }
        }
        if (result.getZone().toString().equals("Z")) {
            result = result.withZoneSameInstant(GMT);
        }
        return result;
    }

    private static ZonedDateTime mungeYear(ZonedDateTime dt) {
        int yr = dt.get(ChronoField.YEAR);
        if (yr < 100 && yr >= 0) {
            if (yr >= 50) {
                yr += 1900;
            } else {
                yr += 2000;
            }
            dt = dt.withYear(yr);
        }
        return dt;
    }

}
