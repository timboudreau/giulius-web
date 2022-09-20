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

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Wraps the interface in the new package with the one in the old one.
 *
 * @author Tim Boudreau
 */
final class WrapperJacksonConfigurer implements JacksonConfigurer, com.mastfrog.jackson.configuration.JacksonConfigurer {

    private final com.mastfrog.jackson.configuration.JacksonConfigurer orig;

    WrapperJacksonConfigurer(com.mastfrog.jackson.configuration.JacksonConfigurer orig) {
        this.orig = orig;
    }

    @Override
    public ObjectMapper configure(ObjectMapper m) {
        return orig.configure(m);
    }

    static WrapperJacksonConfigurer wrap(com.mastfrog.jackson.configuration.JacksonConfigurer orig) {
        return new WrapperJacksonConfigurer(orig);
    }
    
    static boolean wraps(JacksonConfigurer c, Class<?> wrappedType) {
        if (c instanceof WrapperJacksonConfigurer) {
            WrapperJacksonConfigurer wjc = (WrapperJacksonConfigurer) c;
            return wrappedType.isInstance(wjc.orig);
        }
        return false;
    }

}
