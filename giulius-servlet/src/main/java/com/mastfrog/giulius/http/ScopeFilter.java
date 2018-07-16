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

import com.mastfrog.giulius.Dependencies;
import com.mastfrog.giulius.scope.AbstractScope;
import com.mastfrog.util.preconditions.ConfigurationError;
import com.mastfrog.util.preconditions.Checks;
import com.mastfrog.util.function.Invokable;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Servlet filter which runs the remainder of the filter chain inside an
 * AbstractScope instance, with a method for getting the objects which
 * should be injected into that scope.
 * <p/>
 * Subclasses should have a default constructor.  Instance members will be injected
 * by Guice during filter initialization.
 * 
 *
 * @author Tim Boudreau
 */
public abstract class ScopeFilter<S extends AbstractScope> implements Filter {
    protected final S scope;
    protected ScopeFilter (S scope) {
        Checks.notNull("scope", scope);
        this.scope = scope;
    }

    @Override
    public final void init(FilterConfig fc) throws ServletException {
        ServletContext servletContext = fc.getServletContext();
        Dependencies deps = (Dependencies) servletContext.getAttribute(Dependencies.class.getName());
        if (deps == null) {
            throw new ConfigurationError("servletContext should have Dependencies stored "
                    + "as attribute " + Dependencies.class.getName());
        }
        deps.injectMembers(this);
        onInitialization(fc, servletContext);
    }
    
    /**
     * Called during Filter.init(), after requesting injection from Guice
     * @param config The filter config
     * @param context The servlet context
     */
    protected void onInitialization(FilterConfig config, ServletContext context) throws ServletException {
        //do nothing
    }
    
    protected abstract Object[] getScopeContents (ServletRequest request);;

    @Override
    public final void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain fc) throws IOException, ServletException {
        class I extends Invokable<Void, Void, ServletException> {
            I() {
                super (ServletException.class);
            }
            private IOException ioe;

            @Override
            public Void run(Void argument) throws ServletException {
                try {
                    fc.doFilter(request, response);
                } catch (IOException ex) {
                    ioe = ex;
                }
                return null;
            }
        }
        I i = new I();
        scope.<Void,Void,ServletException>run(new I(), null, getScopeContents(request));
        if (i.ioe != null) {
            throw i.ioe;
        }
    }

    @Override
    public void destroy() {
        //do nothing
    }

}
