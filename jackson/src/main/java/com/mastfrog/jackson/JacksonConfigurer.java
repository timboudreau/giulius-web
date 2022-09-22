/* 
 * The MIT License
 *
 * Copyright 2013 Tim Boudreau.
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
 * Implement this interface and register it using &#064;ServiceProvider to allow
 * it to contribute configuration to Jackson on initialization.
 *
 * @author Tim Boudreau
 * @deprecated Use * @see com.mastfrog.jackson.configuration.JacksonConfigurer
 * instead - implementations here are now simply wrappers around types defined
 * in the jackson-configuration library.
 */
@Deprecated
public interface JacksonConfigurer extends Comparable<JacksonConfigurer> {

    /**
     * Configure the passed object mapper
     *
     * @param m The object mapper
     * @return An object mapper
     */
    public ObjectMapper configure(ObjectMapper m);

    /**
     * By default, returns the simple name of the JacksonConfigurer. Do not
     * override unless you are writing a JacksonConfigurer that wrappers another
     * one somehow - this is used to de-duplicate the list of configurers
     * configured in JacksonModule, to ensure there is only one configuration
     * for a given set of types present.
     *
     * @return A name
     */
    default String name() {
        return getClass().getSimpleName();
    }

    /**
     * Arbitrary ordering for to ensure configurers are applied in a particular
     * order in the case of a need to override one with another. The default is
     * 0, which is used by all built-in configurers.
     *
     * @return an int
     */
    default int precedence() {
        return 0;
    }

    /**
     * Implements comparison by precedence.
     *
     * @param o Another configurer
     * @return an int
     */
    @Override
    default int compareTo(JacksonConfigurer o) {
        return Integer.compare(precedence(), o.precedence());
    }

}
