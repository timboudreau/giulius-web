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

import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.mastfrog.giulius.Dependencies;
import com.mastfrog.giulius.DependenciesBuilder;
import com.mastfrog.util.preconditions.Checks;
import com.mastfrog.util.preconditions.Exceptions;
import com.mastfrog.util.streams.Streams;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 *
 * @author Tim Boudreau
 */
public final class WebServer {
    private final String path;
    private final DependenciesBuilder modules;
    private final int port;
    private final Set<Resource> resources;
    private final List<Class<? extends ServletContextListener>> listeners;
    private final File workingDir;
    private LoginService loginService;

    public WebServer(int port, String path, DependenciesBuilder modules, Set<Resource> resources, List<Class<? extends ServletContextListener>> listeners, File workingDir) {
        Checks.nonNegative("port", port);
        this.port = port;
        this.resources = new HashSet<>(resources);
        this.listeners = listeners;
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        this.path = path;
        this.modules = modules;
        this.workingDir = workingDir;
    }

    public URL getBaseURL() throws MalformedURLException {
        return new URL("http://127.0.0.1:" + port);
    }

    public String toString() {
        return "Embeded Jetty web server on " + port + " serving from http://localhost:" + port + path + " with \n" + modules;
    }

    public String getPath() {
        return path;
    }

    public int getPort() {
        return port;
    }
    private static ThreadLocal<WebServer> serverLocal = new ThreadLocal<>();
    static ThreadLocal<ServletContext> ctxLocal = new ThreadLocal<>();

    public void stop() throws Exception {
        try {
            if (ctx != null) {
                ctx.destroy();
            }
        } finally {
            if (server != null) {
                server.stop();
            }
        }
    }
    private Server server;
    private WebAppContext ctx;
    
    public synchronized void setLoginService(LoginService svc) {
        this.loginService = svc;
    }

    public synchronized void start() throws Exception {
        if (server != null) {
            throw new IllegalStateException("Already started");
        }
        try {
            Logger.getLogger("com").setLevel(Level.ALL);
            Logger.getLogger("").setLevel(Level.ALL);

            serverLocal.set(this);
            server = new Server();

            ServerConnector connector = new ServerConnector(server);
            connector.setPort(port);
            server.addConnector(connector);

            WebAppContext webApp = new Ctx(".", "/");

            if (loginService != null) {
                webApp.getSecurityHandler().setLoginService(loginService);
            }
            if (workingDir != null) {
                webApp.setResourceBase(workingDir.getAbsolutePath());
            }
            for (Resource res : resources) {
                webApp.setResourceAlias(res.toURL().toURI().toString(), res.getPath());
            }

            webApp.addEventListener(new Ini());
            for (Class<? extends ServletContextListener> ltype : listeners) {
                webApp.addEventListener(ltype.newInstance());
            }

            ServletHandler handler = createServletHandler();
            webApp.setServletHandler(handler);
            
            server.setHandler(webApp);

            System.out.println("Start embedded web server on port " + port + " at " + path);
            server.start();

        } finally {
            serverLocal.remove();
            ctxLocal.remove();
        }
    }
    class Ctx extends WebAppContext {
        public Ctx(String webApp, String contextPath) {
            super(webApp, contextPath);
        }

        @Override
        public org.eclipse.jetty.util.resource.Resource getResource(String uriInContext) throws MalformedURLException {
            System.out.println("GET RESOURCE " + uriInContext);
            for (final Resource res : WebServer.this.resources) {
                if (res.match(uriInContext)) {
                    return new org.eclipse.jetty.util.resource.Resource() {
                        @Override
                        public void close() {
                            //do nothing
                        }

                        @Override
                        public boolean exists() {
                            return true;
                        }

                        @Override
                        public boolean isDirectory() {
                            return false;
                        }

                        @Override
                        public long lastModified() {
                            return 0;
                        }

                        @Override
                        public long length() {
                            return -1;
                        }

                        @Override
                        public URL getURL() {
                            try {
                                return res.toURL();
                            } catch (IOException ex) {
                                Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
                                return null;
                            }
                        }

                        @Override
                        public File getFile() throws IOException {
                            throw new UnsupportedOperationException("Not supported yet.");
                        }

                        @Override
                        public String getName() {
                            return res.getPath();
                        }

                        @Override
                        public InputStream getInputStream() throws IOException {
                            return res.toStream();
                        }

                        @Override
                        public boolean delete() throws SecurityException {
                            throw new UnsupportedOperationException("Not supported yet.");
                        }

                        @Override
                        public String[] list() {
                            return new String[0];
                        }

                        @Override
                        public org.eclipse.jetty.util.resource.Resource addPath(String path) throws IOException, MalformedURLException {
                            return null;
                        }

                        @Override
                        public boolean isContainedIn(org.eclipse.jetty.util.resource.Resource r) throws MalformedURLException {
                            throw new UnsupportedOperationException("Not supported yet.");
                        }

                        @Override
                        public boolean renameTo(org.eclipse.jetty.util.resource.Resource dest) throws SecurityException {
                            throw new UnsupportedOperationException("Not supported yet.");
                        }

                        @Override
                        public ReadableByteChannel getReadableByteChannel() throws IOException {
                            return Streams.asByteChannel(getInputStream());
                        }
                    };
                }
            }
            return super.getResource(uriInContext);
        }
    }

    private ServletHandler createServletHandler() {
        ServletHandler servletHandler = new ServletHandler();

        FilterHolder guiceFilterHolder = createGuiceFilterHolder();
        servletHandler.addFilter(guiceFilterHolder,
                createFilterMapping("/*", guiceFilterHolder));

        return servletHandler;
    }

    private FilterHolder createGuiceFilterHolder() {
        FilterHolder filterHolder = new FilterHolder(GF.class);
        filterHolder.setName("guice");
        return filterHolder;
    }

    private FilterMapping createFilterMapping(
            String pathSpec, FilterHolder filterHolder) {
        FilterMapping filterMapping = new FilterMapping();
        filterMapping.setPathSpec(pathSpec);
        filterMapping.setFilterName(filterHolder.getName());
        return filterMapping;
    }

    public void run() throws Exception {
        server.start();
        server.join();
    }
    public static class GF extends GuiceFilter {
        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
            try {
                super.doFilter(servletRequest, servletResponse, filterChain);
            } catch (RuntimeException | IOException | ServletException e) {
                e.printStackTrace();
                throw e;
            }
        }

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
            super.init(filterConfig);
        }
    }
    public static class Ini implements ServletContextListener {
        Dependencies deps;
        private final WebServer server;

        public Ini() {
            WebServer svr = serverLocal.get();
            if (svr == null) {
                throw new Error("Created out of context");
            }
            this.server = svr;
        }

        @Override
        public void contextInitialized(ServletContextEvent sce) {
            System.out.println("INIT CONTEXT");
            try {
                ctxLocal.set(sce.getServletContext());
                try {
                    deps = server.modules.build();
                    ServletContext ctx = sce.getServletContext();
                    ctx.setAttribute(Injector.class.getName(), deps.getInjector());
                    ctx.setAttribute(Dependencies.class.getName(), deps);
                    System.out.println("CONTEXT INIT");
                } finally {
                    ctxLocal.remove();
                }
            } catch (IOException ex) {
                Exceptions.chuck(ex);
            }
        }

        @Override
        public void contextDestroyed(ServletContextEvent sce) {
            deps.shutdown();
        }
    }
}
