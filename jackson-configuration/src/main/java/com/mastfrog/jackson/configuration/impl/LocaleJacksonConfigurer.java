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
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mastfrog.jackson.configuration.JacksonConfigurer;
import com.mastfrog.util.service.ServiceProvider;
import java.io.IOException;
import java.util.Locale;

/**
 * By default, Jackson serializes locales using Java constant names, so the
 * hyphens in the ISO standard are replaced with underscores, breaking
 * cross-language portability. This on-by-default serializer ensures they are
 * serialized correctly both as keys and values. Note, this class is public
 * because it is registered in META-INF/services. This package will be hidden
 * once this library begins using the Java Module System.
 *
 * @author Tim Boudreau
 */
@ServiceProvider(JacksonConfigurer.class)
public final class LocaleJacksonConfigurer implements JacksonConfigurer {

    private static final LocaleSerializer LOCALE_SERIALIZER = new LocaleSerializer();
    private static final LocaleKeySerializer LOCALE_KEY_SERIALIZER = new LocaleKeySerializer();
    private static final LocaleDeserializer LOCALE_DESERIALIZER = new LocaleDeserializer();
    private static final LocaleKeyDeserializer LOCALE_KEY_DESERIALIZER = new LocaleKeyDeserializer();

    @Override
    public ObjectMapper configure(ObjectMapper om) {
        SimpleModule sm = new SimpleModule("optional1", new Version(1, 0, 0, null, "com.mastfrog", "java-locale-serializer"));
        sm.addKeyDeserializer(Locale.class, LOCALE_KEY_DESERIALIZER);
        sm.addKeySerializer(Locale.class, LOCALE_KEY_SERIALIZER);
        sm.addDeserializer(Locale.class, LOCALE_DESERIALIZER);
        sm.addSerializer(Locale.class, LOCALE_SERIALIZER);
        om.registerModule(sm);
        return om;
    }
    
    public String toString() {
        return "LocaleJacksonConfigurer";
    }

    private static final class LocaleDeserializer extends JsonDeserializer<Locale> {

        @Override
        public boolean isCachable() {
            return true;
        }

        @Override
        public Class<?> handledType() {
            return Locale.class;
        }

        @Override
        public Locale deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            String string = jp.readValueAs(String.class);
            return string.isEmpty() ? Locale.ROOT 
                    : Locale.forLanguageTag(replaceUnderscore(string));
        }
    }

    private static final class LocaleSerializer extends JsonSerializer<Locale> {

        @Override
        public Class<Locale> handledType() {
            return Locale.class;
        }

        @Override
        public void serialize(Locale t, JsonGenerator jg, SerializerProvider sp) throws IOException,
                JsonProcessingException {
            jg.writeString(t.toLanguageTag());
        }
    }

    private static final class LocaleKeySerializer extends JsonSerializer<Locale> {

        @Override
        public Class<Locale> handledType() {
            return Locale.class;
        }

        @Override
        public void serialize(Locale t, JsonGenerator jg, SerializerProvider sp) throws IOException,
                JsonProcessingException {
            jg.writeFieldName(t.toLanguageTag());
        }

    }

    private static final class LocaleKeyDeserializer extends KeyDeserializer {

        @Override
        public Object deserializeKey(String string, DeserializationContext dc) throws IOException,
                JsonProcessingException {
            if (string.isEmpty()) {
                return Locale.ROOT;
            }
            return Locale.forLanguageTag(replaceUnderscore(string));
        }
    }
    
    static String replaceUnderscore(String string) {
            if (string.length() > 3 && string.charAt(2) == '_') {
                // Ensure legacy entries endcoded with _ can be decoded
                char[] chars = string.toCharArray();
                chars[2] = '-';
                string = new String(chars);
            }
            return string;
    }
}
