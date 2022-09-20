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

/**
 *
 * @author Tim Boudreau
 * @deprecated Use com.mastfrog.jackson.configuration.DurationSerializationMode
 * instead
 * @see com.mastfrog.jackson.configuration.DurationSerializationMode
 */
@Deprecated
public enum DurationSerializationMode {
    /**
     * Store durations as a number of milliseconds.
     */
    DURATION_AS_MILLIS,
    /**
     * Store duration as a <code>:</code> and <code>.</code> delimited string,
     * e.g. <code>02:35:10.032</code> for two hours, thirty five minutes, ten
     * seconds and 32 milliseconds.
     */
    DURATION_AS_STRING,
    /**
     * Store durations in the ISO format returned by Duration.toString and
     * parsed by Duration.parse.
     */
    DURATION_AS_ISO_STRING,
    /**
     * Do not configure serialization for durations (you have some other
     * serializer that will do it).
     */
    NONE;

    com.mastfrog.jackson.configuration.DurationSerializationMode convert() {
        switch (this) {
            case DURATION_AS_ISO_STRING:
                return com.mastfrog.jackson.configuration.DurationSerializationMode.DURATION_AS_ISO_STRING;
            case DURATION_AS_MILLIS:
                return com.mastfrog.jackson.configuration.DurationSerializationMode.DURATION_AS_MILLIS;
            case DURATION_AS_STRING:
                return com.mastfrog.jackson.configuration.DurationSerializationMode.DURATION_AS_STRING;
            case NONE:
                return com.mastfrog.jackson.configuration.DurationSerializationMode.NONE;
            default:
                throw new AssertionError(this);
        }
    }

    static DurationSerializationMode forAlternate(com.mastfrog.jackson.configuration.DurationSerializationMode mode) {
        switch (mode) {
            case DURATION_AS_ISO_STRING:
                return DURATION_AS_ISO_STRING;
            case DURATION_AS_MILLIS:
                return DURATION_AS_MILLIS;
            case DURATION_AS_STRING:
                return DURATION_AS_STRING;
            case NONE:
                return NONE;
            default:
                throw new AssertionError(mode);
        }
    }
}
