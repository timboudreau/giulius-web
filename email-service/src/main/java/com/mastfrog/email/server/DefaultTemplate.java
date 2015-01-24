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
package com.mastfrog.email.server;

import com.google.inject.ImplementedBy;

/**
 *
 * @author Tim Boudreau
 */
@ImplementedBy(DefaultHtmlTemplateProvider.DefaultTemplateImpl.class)
public abstract class DefaultTemplate {

    protected abstract String resourceName();

    protected abstract Class<?> relativeTo();

    public static DefaultTemplate create(Class<?> relativeTo, String resourceName) {
        return new DTImpl(relativeTo, resourceName);
    }

    private static final class DTImpl extends DefaultTemplate {

        private final Class<?> relativeTo;
        private final String resourceName;

        private DTImpl(Class<?> relativeTo, String resourceName) {
            this.relativeTo = relativeTo;
            this.resourceName = resourceName;
        }

        @Override
        protected String resourceName() {
            return resourceName;
        }

        @Override
        protected Class<?> relativeTo() {
            return relativeTo;
        }

    }
}
