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

import com.google.inject.Binder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.servlet.GuiceServletContextListener;
import com.mastfrog.settings.Settings;
import com.mastfrog.settings.SettingsBuilder;
import com.mastfrog.giulius.Dependencies;
import com.mastfrog.giulius.DependenciesBuilder;
import com.mastfrog.giulius.annotations.Namespace;
import com.mastfrog.util.preconditions.Exceptions;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Servlet listener which configures Guice
 *
 */
public abstract class GuiceBootstrapServletListener extends GuiceServletContextListener {

    private ServletContext servletContext;
    private volatile boolean configuring;

    private final class CurrentServletContextImpl implements CurrentServletContext {

        @Override
        public ServletContext getContext() {
            synchronized (GuiceBootstrapServletListener.this) {
                return servletContext;
            }
        }
    }

    /**
     * Configures {@link CurrentServletContext} to an implementation that
     * returns the servlet context of this servlet context listener.
     */
    private final class ServletContextModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(CurrentServletContext.class).toInstance(
                    new CurrentServletContextImpl());
            bind(CurrentHttpRequest.class).toProvider(
                    CurrentHttpRequestProvider.class);

        }
    }

    @Override
    public final synchronized void contextInitialized(ServletContextEvent servletContextEvent) {
        onBeforeContextInitialized(servletContextEvent);
        // set context before calling super so that the servlet context is
        // available while creating the injector (which is done in super)
        this.servletContext = servletContextEvent.getServletContext();
        super.contextInitialized(servletContextEvent);
        onAfterContextInitialized(servletContextEvent);
    }

    @Override
    public final synchronized void contextDestroyed(ServletContextEvent servletContextEvent) {
        onBeforeContextDestroyed(servletContextEvent);
        Dependencies deps = (Dependencies) servletContextEvent.getServletContext().getAttribute(Dependencies.class.getName());
        if (deps != null) {
            deps.shutdown();
        }
        super.contextDestroyed(servletContextEvent);
        servletContext.setAttribute(Dependencies.class.getName(), null);
        servletContext.setAttribute(Injector.class.getName(), null);
        servletContext = null;
        onAfterContextDestroyed(servletContextEvent);
    }

    protected SettingsBuilder createSettingsBuilder(ServletContext ctx) {
        return SettingsBuilder.createDefault();
    }

    protected void configureInitParameters(SettingsBuilder sb, ServletContext ctx) {
        Properties props = new Properties();
        for (Enumeration<?> en = servletContext.getInitParameterNames(); en.hasMoreElements();) {
            Object o = en.nextElement();
            String key = o == null ? null : (String) o;
            if (key != null) {
                String val = servletContext.getInitParameter(key);
                if (val != null) {
                    props.setProperty(key, val);
                }
            }
        }
        if (!props.isEmpty()) {
            sb.add(props);
        }
    }

    protected Settings createSettings(ServletContext context) {
        try {
            SettingsBuilder sb = createSettingsBuilder(context);
            configureInitParameters(sb, context);
            //use mutable just in case old code wants to mutate things
            return sb.buildMutableSettings();
        } catch (IOException ex) {
            throw new Error(ex);
        }
    }

    private final class InternalModule implements Module {

        private final Settings settings;
        private final ServletContext context;

        InternalModule(Settings settings, ServletContext context) {
            this.settings = settings;
            this.context = context;
        }

        @Override
        public void configure(Binder binder) {
            bind(settings, binder, context);
        }
    }

    /**
     * Override to perform additional bindings
     *
     * @param binder The Guice binder
     * @param settings The settings
     */
    protected void bind(Settings settings, Binder binder, ServletContext context) {
        //do nothing
    }

    @Override
    protected final Injector getInjector() {
        assert servletContext != null;
        assert Thread.holdsLock(this);
        if (configuring) {
            throw new Error("Override onInjectorCreated - getInjector cannot "
                    + "be recursively called");
        }
        configuring = true;
        try {
            DependenciesBuilder bldr;
            List<Module> modules = new ArrayList<Module>();
//            modules.add(new ServletContextModule());
            try {
                Settings settings = configure(servletContext, modules);
                if (settings == null) {
                    settings = createSettings(servletContext);
                }
                bldr = builder(settings, servletContext);
                for (Module m : modules) {
                    bldr.add(m);
                }
                bldr.add(settings, Namespace.DEFAULT);
                onBeforeCreateDependencies(bldr);
                System.out.println(bldr);
                Dependencies dependencies = bldr.build();
                servletContext.setAttribute(Dependencies.class.getName(), dependencies);
                onDependenciesCreated(settings, dependencies, servletContext);
                Injector injector = dependencies.getInjector();
                servletContext.setAttribute(Injector.class.getName(), injector);
                onInjectorCreated(injector, servletContext, dependencies.getStage());
                return injector;
            } catch (Exception e) {
                return Exceptions.chuck(e);
            }
        } finally {
            configuring = false;
        }
    }

    /**
     * Set up any namespaces or other things which need to be configured here.
     * This is called before injection, on application startup/servlet
     * initialization. You can customize the set of Guice modules or anything
     * else you need to by adding things to the DependenciesBuilder.
     * <p/>
     * Note: If you want to override the default namespace's settings, you will
     * want to implement configure() to do that.
     *
     * @param builder A DependenciesBuilder which is being used to configure the
     * application
     */
    protected void onBeforeCreateDependencies(DependenciesBuilder builder) {
        //do nothing
    }

    protected DependenciesBuilder builder(Settings settings, ServletContext servletContext) {
        return Dependencies.builder().add(new ServletContextModule()).add(new InternalModule(settings, servletContext));
    }

    /**
     * Called when the injector is created, at the end of initialization
     *
     * @param injector
     */
    protected void onInjectorCreated(Injector injector, ServletContext context, Stage stage) {
        //do nothing
    }

    /**
     * Lets extending base classes {@link #setSettings(Settings) set the
     * settings} and {@link #setModules(Module...) modules} to use to create a
     * {@link Dependencies}/ {@link Injector} object with. The default
     * implementation returns null.
     *
     * @param context The servlet context
     * @param addModulesHere A list of modules which can be added to
     * @return A Settings (if you want to customize settings somehow based on
     * the servlet context), or null if the default new Settings() should be
     * used
     */
    protected Settings configure(ServletContext context, List<? super Module> addModulesHere) throws Exception {
        return null;
    }

    /**
     * Template method that is called right after the {@link Dependencies}
     * object is created. You can override this in case you want to do
     * additional (manual) bootstrapping based on the dependences object.
     *
     * @param settings settings used to construct the dependencies object
     * @param dependencies the newly constructed dependencies object
     */
    protected void onDependenciesCreated(Settings settings,
            Dependencies dependencies, ServletContext context) {
    }

    /**
     * Callback method for subclasses to hook into lifecycle events
     *
     * @param servletContextEvent
     */
    protected void onBeforeContextInitialized(ServletContextEvent servletContextEvent) {
    }

    /**
     * Callback method for subclasses to hook into lifecycle events
     *
     * @param servletContextEvent
     */
    protected void onAfterContextInitialized(ServletContextEvent servletContextEvent) {
    }

    /**
     * Callback method for subclasses to hook into lifecycle events
     *
     * @param servletContextEvent
     */
    protected void onBeforeContextDestroyed(ServletContextEvent servletContextEvent) {
    }

    /**
     * Callback method for subclasses to hook into lifecycle events
     *
     * @param servletContextEvent
     */
    protected void onAfterContextDestroyed(ServletContextEvent servletContextEvent) {
    }
}
