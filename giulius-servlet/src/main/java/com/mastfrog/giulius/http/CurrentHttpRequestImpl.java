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
package com.mastfrog.giulius.http;

import javax.servlet.http.HttpServletRequest;

/**
 * Context of the current request.
 *
 * @author hillenius
 */
class CurrentHttpRequestImpl implements CurrentHttpRequest {

    /**
     * thread local for keeping a reference to the HTTP request instance linked to a request if we're in the middle of one.
     */
    private static final ThreadLocal<CurrentHttpRequestImpl> current = new ThreadLocal<CurrentHttpRequestImpl>();

    /**
     * Sets the context for this thread. Clients that set this need to ensure unset is called after we're done with the request.
     *
     * @param ctx context
     */
    static void set(CurrentHttpRequestImpl ctx) {
        current.set(ctx);
    }

    /**
     * Un-sets the context for this thread.
     */
    static void unset() {
        current.remove();
    }

    /**
     * @return current request context. Is set by {@link CurrentHttpRequestFilter}.
     */
    static CurrentHttpRequestImpl get() {
        return current.get();
    }
    /**
     * HTTP request.
     */
    private final HttpServletRequest request;

    /**
     * Create with request.
     * @param request HTTP request
     */
    protected CurrentHttpRequestImpl(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * @return the HTTP request
     */
    public HttpServletRequest getRequest() {
        return request;
    }

    @Override
    public String toString() {
        return "RequestContext [request=" + request + "]";
    }
}
