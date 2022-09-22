/*
 * The MIT License
 *
 * Copyright 2015 Tim Boudreau.
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mastfrog.jackson.configuration.JacksonConfigurer;
import com.mastfrog.util.service.ServiceProvider;
import java.io.IOException;
import java.util.Optional;

/**
 * Serializes java.util.Optional to its value or null. No deserializer is
 * provided, so this is one-way. Note, this class is public because it is
 * registered in META-INF/services. This package will be hidden once this
 * library begins using the Java Module System.
 *
 * @author Tim Boudreau
 */
@ServiceProvider(JacksonConfigurer.class)
public class JavaOptionalSerializer implements JacksonConfigurer {

    @Override
    public ObjectMapper configure(ObjectMapper mapper) {
        SimpleModule sm = new SimpleModule("java-optional", new Version(1, 0, 0, null, "com.mastfrog", "com-google-common-base-optional"));
        sm.addSerializer(new OptionalSer());
        mapper.registerModule(sm);
        return mapper;
    }

    @Override
    public int precedence() {
        return 0;
    }
    
    @Override
    public String toString() {
        return "JavaOptionalSerializer";
    }

    @SuppressWarnings("unchecked")
    private static final class OptionalSer extends JsonSerializer<Optional> {

        @Override
        public Class<Optional> handledType() {
            return Optional.class;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void serialize(Optional t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            if (t.isPresent()) {
                sp.defaultSerializeValue(t.get(), jg);
            } else {
                jg.writeNull();
            }
        }
    }
}
