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

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author Tim Boudreau
 * @deprecated Implementation moved to the jackson-configuration library, which
 * does not depend on Guice. Use
 * com.mastfrog.jackson.configuration.JacksonConfigurer.javaTimeConfigurer
 * instead.
 */
@Deprecated
public class JavaTimeConfigurer implements JacksonConfigurer {

    // Original implementation is moved to here:
    private final com.mastfrog.jackson.configuration.JacksonConfigurer delegate;

    public JavaTimeConfigurer() {
        this(TimeSerializationMode.TIME_AS_EPOCH_MILLIS,
                DurationSerializationMode.DURATION_AS_MILLIS);
    }

    public JavaTimeConfigurer(TimeSerializationMode mode, DurationSerializationMode durationMode) {
        this.delegate = com.mastfrog.jackson.configuration.JacksonConfigurer.javaTimeConfigurer(mode.convert(),
                durationMode.convert());
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public ObjectMapper configure(ObjectMapper mapper) {
        return delegate.configure(mapper);
    }

    @Override
    public String toString() {
        return "JTC(" + delegate + ")";
    }

}
