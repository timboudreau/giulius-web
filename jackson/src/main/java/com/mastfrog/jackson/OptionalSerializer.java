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
package com.mastfrog.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Optional;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service = JacksonConfigurer.class)
public class OptionalSerializer implements JacksonConfigurer {

    @Override
    public ObjectMapper configure(ObjectMapper mapper) {
        SimpleModule sm = new SimpleModule("optional", new Version(1, 0, 0, null, "com.mastfrog", "jackson"));
        sm.addSerializer(new OptionalSer());
        sm.addSerializer(new ReflectionOptionalSerializer());
        mapper.registerModule(sm);
        return mapper;
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
    
    private static final class ReflectionOptionalSerializer extends JsonSerializer {

        static Class<?> optionalType;
        static boolean failed;
        static Method GET_METHOD;
        static Method IS_PRESENT_METHOD;
        ReflectionOptionalSerializer() {
            if (optionalType == null && !failed) {
                try {
                    optionalType = Class.forName("java.util.Optional");
                    GET_METHOD = optionalType.getMethod("get");
                    IS_PRESENT_METHOD = optionalType.getMethod("isPresent");
                } catch (ClassNotFoundException ex) {
                    failed = true;
                    optionalType = Nothing.class;
                } catch (NoSuchMethodException ex) {
                    failed = true;
                    optionalType = Nothing.class;
                } catch (SecurityException ex) {
                    failed = true;
                    optionalType = Nothing.class;
                }
            }
        }

        @Override
        public Class handledType() {
            return optionalType;
        }
        

        @Override
        public void serialize(Object t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            if (failed) {
                return;
            }
            try {
                Boolean present = (Boolean) IS_PRESENT_METHOD.invoke(t);
                if (present) {
                    Object val = GET_METHOD.invoke(t);
                    sp.defaultSerializeValue(val, jg);
                } else {
                    jg.writeNull();
                }
            } catch (IllegalAccessException ex) {
                Logger.getLogger(OptionalSerializer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(OptionalSerializer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvocationTargetException ex) {
                Logger.getLogger(OptionalSerializer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        private static final class Nothing {
            private Nothing(){};
        }
    }
}
