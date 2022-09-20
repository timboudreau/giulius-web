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
package com.mastfrog.jackson.configuration;

/**
 * Serialization modes that can be passed to JavaTimeConfigurer.
 *
 * @author Tim Boudreau
 */
public enum TimeSerializationMode {
    /**
     * Store timestamps as unix epoch milliseconds.
     */
    TIME_AS_EPOCH_MILLIS,
    /**
     * Store timestamps as ISO 8601 format strings.
     */
    TIME_AS_ISO_STRING,
    /**
     * Read and store timestamps in the ISO2822 format used for HTTP headers.
     * Note that the parser implementation for these is <b>very</b>
     * permissive, owing to the variety of (mis) interpretations of that spec
     * that exist. Also note that this format has no provision for milliseconds,
     * which will be returned as zero.
     */
    HTTP_HEADER_FORMAT,
    /**
     * Do not configure time serialization (either you don't need it, or
     * something else is already taking care of that).
     */
    NONE;

    public boolean isMillisecondResolution() {
        return this != HTTP_HEADER_FORMAT;
    }
}
