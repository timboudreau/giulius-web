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
package com.mastfrog.grizzly;

import com.google.inject.Module;
import com.google.inject.servlet.ServletModule;
import com.mastfrog.giulius.DependenciesBuilder;
import com.mastfrog.settings.Settings;
import static com.mastfrog.settings.SettingsBuilder.DEFAULT_NAMESPACE;
import com.mastfrog.util.preconditions.Checks;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServlet;

/**
 *
 * @author Tim Boudreau
 */
public final class WebServerBuilder {
    private final Set<Resource> resources = new HashSet<>();
    private final List<Class<? extends ServletContextListener>> listeners = new ArrayList<>();
    private final int port;
    public static final int DEFAULT_PORT = 9191;
    private final DependenciesBuilder deps = new DependenciesBuilder();
    private final String path;
    private boolean settingsSet;
    private File workingDir;

    public WebServerBuilder(int port, String path) {
        this.port = port;
        this.path = path;
    }

    public WebServerBuilder(int port) {
        this(port, "/");
    }

    public WebServerBuilder(String path) {
        this(DEFAULT_PORT, path);
    }

    public WebServerBuilder() {
        this(DEFAULT_PORT);
    }

    public WebServerBuilder setWorkingDir(String path) {
        return setWorkingDir(new File(path));
    }

    public WebServerBuilder setWorkingDir(File path) {
        Checks.notNull("path", path);
        Checks.folderExists(path);
        if (workingDir != null) {
            throw new IllegalStateException("Resource directory already set to " + workingDir);
        }
        workingDir = path;
        return this;
    }

    public WebServerBuilder add(Settings settings) {
        return add(DEFAULT_NAMESPACE, settings);
    }

    public WebServerBuilder add(String namespace, Settings settings) {
        deps.add(settings, namespace);
        settingsSet = true;
        return this;
    }

    public WebServerBuilder add(Class<? extends ServletContextListener> listener) {
        listeners.add(listener);
        return this;
    }

    public WebServerBuilder add(Resource resource) {
        if (resources.contains(resource)) {
            throw new IllegalArgumentException("Already have a resource " + resource);
        }
        resources.add(resource);
        return this;
    }

    public WebServerBuilder add(Module... modules) {
        System.out.println("add module " + Arrays.asList(modules));
        deps.add(modules);
        return this;
    }

    public WebServerFilterBuilder add(final String path, final Class<? extends Filter> filterType, final String... morePaths) {
        return new WebServerFilterBuilder(null, filterType, path, morePaths);
    }

    public WebServerFilterBuilder add(final String path, final Filter filter, final String... morePaths) {
        return new WebServerFilterBuilder(filter, null, path, morePaths);
    }

    public WebServerServletBuilder add(final Class<? extends HttpServlet> servlet, final String path, final String... morePaths) {
        return new WebServerServletBuilder(null, servlet, path, morePaths);
    }

    public WebServerServletBuilder add(final HttpServlet servlet, final String path, final String... morePaths) {
        return new WebServerServletBuilder(servlet, null, path, morePaths);
    }
    
    public WebServer build() {
        if (!settingsSet) {
            try {
                deps.addDefaultSettings();
            } catch (IOException ex) {
                Logger.getLogger(WebServerBuilder.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return new WebServer(port, path, deps, resources, listeners, workingDir);
    }
    public class WebServerFilterBuilder {
        private final Map<String, String> params = new HashMap<>();
        private final Filter filter;
        private final Class<? extends Filter> clazz;
        private final String path;
        private final String[] morePaths;
        private Map<String, Object> attributes = new HashMap<>();

        WebServerFilterBuilder(Filter servlet, Class<? extends Filter> clazz, String path, String... morePaths) {
            assert servlet != null || clazz != null;
            this.filter = servlet;
            this.clazz = clazz;
            this.path = path;
            this.morePaths = morePaths;
        }

        public WebServerBuilder done() {
            if (clazz != null) {
                return WebServerBuilder.this.add(new ServletModule() {
                    @Override
                    protected void configureServlets() {
                        filter(path, morePaths).through(clazz, params);
                        if (!attributes.isEmpty()) {
//                            ServletContext ctx = getServletContext();
                            ServletContext ctx = null;
                            if (ctx == null) {
                                ctx = WebServer.ctxLocal.get();
                                if (ctx == null) {
                                    throw new Error("Not being instantiated inside GuiceFilter");
                                }
                            }
                            for (Map.Entry<String, Object> e : attributes.entrySet()) {
                                ctx.setAttribute(e.getKey(), e.getValue());
                            }
                        }
                    }

                    @Override
                    public String toString() {
                        return "Filter " + clazz.getName();
                    }
                });
            } else if (filter != null) {
                return WebServerBuilder.this.add(new ServletModule() {
                    @Override
                    protected void configureServlets() {
                        filter(path, morePaths).through(filter, params);
                        if (!attributes.isEmpty()) {
//                            ServletContext ctx = getServletContext();
                            ServletContext ctx = null;
                            if (ctx == null) {
                                ctx = WebServer.ctxLocal.get();
                                if (ctx == null) {
                                    throw new Error("Not being instantiated inside GuiceFilter");
                                }
                            }
                            for (Map.Entry<String, Object> e : attributes.entrySet()) {
                                ctx.setAttribute(e.getKey(), e.getValue());
                            }
                        }
                    }

                    @Override
                    public String toString() {
                        return "Filter " + filter;
                    }
                });
            } else {
                throw new AssertionError();
            }
        }

        public WebServerFilterBuilder withAttribute(String key, Object val) {
            attributes.put(key, val);
            return this;
        }

        public WebServerFilterBuilder add(final String path, final Class<? extends Filter> filterType, final String... morePaths) {
            return done().add(path, filterType, morePaths);
        }

        public WebServerFilterBuilder add(final String path, final Filter filter, final String... morePaths) {
            return done().add(path, filter, morePaths);
        }

        public WebServerServletBuilder add(final HttpServlet servlet, final String path, final String... morePaths) {
            return done().add(servlet, path, morePaths);
        }

        public WebServerServletBuilder add(final Class<? extends HttpServlet> servlet, final String path, final String... morePaths) {
            return done().add(servlet, path, morePaths);
        }

        public WebServerBuilder add(Module... modules) {
            return WebServerBuilder.this.add(modules);
        }

        public WebServerFilterBuilder initializeWith(String key, String value) {
            params.put(key, value);
            return this;
        }

        public WebServer build() {
            return done().build();
        }

        public WebServerBuilder add(Settings settings) {
            return add(DEFAULT_NAMESPACE, settings);
        }

        public WebServerBuilder add(String namespace, Settings settings) {
            deps.add(settings, namespace);
            return done();
        }

        public WebServerBuilder add(Class<? extends ServletContextListener> listener) {
            listeners.add(listener);
            return done();
        }
    }
    public class WebServerServletBuilder {
        private final Map<String, String> params = new HashMap<>();
        private final HttpServlet servlet;
        private final Class<? extends HttpServlet> clazz;
        private final String path;
        private final String[] morePaths;
        private Map<String, Object> attributes = new HashMap<>();

        WebServerServletBuilder(HttpServlet servlet, Class<? extends HttpServlet> clazz, String path, String... morePaths) {
            assert servlet != null || clazz != null;
            this.servlet = servlet;
            this.clazz = clazz;
            this.path = path;
            this.morePaths = morePaths;
        }

        WebServerBuilder done() {
            if (clazz != null) {
                return WebServerBuilder.this.add(new ServletModule() {
                    @Override
                    protected void configureServlets() {
                        serve(path, morePaths).with(clazz, params);
                        if (!attributes.isEmpty()) {
                            ServletContext ctx = null;
//                            ServletContext ctx = getServletContext();
                            if (ctx == null) {
                                ctx = WebServer.ctxLocal.get();
                                if (ctx == null) {
                                    throw new Error("Not being instantiated inside GuiceFilter");
                                }
                            }
                            for (Map.Entry<String, Object> e : attributes.entrySet()) {
                                ctx.setAttribute(e.getKey(), e.getValue());
                            }
                        }
                    }

                    @Override
                    public String toString() {
                        return "Servlet " + clazz.getName();
                    }
                });
            } else if (servlet != null) {
                return WebServerBuilder.this.add(new ServletModule() {
                    @Override
                    protected void configureServlets() {
                        serve(path, morePaths).with(servlet, params);
                        if (!attributes.isEmpty()) {
//                            ServletContext ctx = getServletContext();
                            ServletContext ctx = null;
                            if (ctx == null) {
                                ctx = WebServer.ctxLocal.get();
                                if (ctx == null) {
                                    throw new Error("Not being instantiated inside GuiceFilter");
                                }
                            }
                            for (Map.Entry<String, Object> e : attributes.entrySet()) {
                                ctx.setAttribute(e.getKey(), e.getValue());
                            }
                        }
                    }

                    @Override
                    public String toString() {
                        return "Servlet " + servlet;
                    }
                });
            } else {
                throw new AssertionError("Both servlet and clazz are null");
            }
        }

        public WebServerServletBuilder withAttribute(String key, Object val) {
            attributes.put(key, val);
            return this;
        }

        public WebServerServletBuilder add(final HttpServlet servlet, final String path, final String... morePaths) {
            return done().add(servlet, path, morePaths);
        }

        public WebServerServletBuilder add(final Class<? extends HttpServlet> servlet, final String path, final String... morePaths) {
            return done().add(servlet, path, morePaths);
        }

        public WebServerBuilder add(Module... modules) {
            return WebServerBuilder.this.add(modules);
        }

        public WebServerFilterBuilder add(final String path, final Class<? extends Filter> filterType, final String... morePaths) {
            return done().add(path, filterType, morePaths);
        }

        public WebServerFilterBuilder add(final String path, final Filter filter, final String... morePaths) {
            return done().add(path, filter, morePaths);
        }

        public WebServerServletBuilder initializeWith(String key, String value) {
            params.put(key, value);
            return this;
        }

        public WebServer build() {
            return done().build();
        }

        public WebServerBuilder add(Settings settings) {
            return add(DEFAULT_NAMESPACE, settings);
        }

        public WebServerBuilder add(String namespace, Settings settings) {
            deps.add(settings, namespace);
            return done();
        }

        public WebServerBuilder add(Class<? extends ServletContextListener> listener) {
            listeners.add(listener);
            return done();
        }
    }
}
