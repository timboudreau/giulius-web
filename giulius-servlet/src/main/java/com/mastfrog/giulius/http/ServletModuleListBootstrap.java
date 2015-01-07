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

import com.google.inject.Module;
import com.mastfrog.util.ConfigurationError;
import com.mastfrog.settings.Settings;
import com.mastfrog.settings.SettingsBuilder;
import com.mastfrog.util.Streams;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import javax.servlet.ServletContext;

/**
 * Simple ServletListener which bootstraps Guice, using a servlet initialization
 * parameter for the list of modules.
 *
 * @author Tim Boudreau
 */
public class ServletModuleListBootstrap extends GuiceBootstrapServletListener {

    private final String moduleListInitParam;
    public static final String DEFAULT_INIT_PARAM = "guice.modules";

    protected ServletModuleListBootstrap(String moduleListInitParam) {
        this.moduleListInitParam = moduleListInitParam;
    }

    public ServletModuleListBootstrap() {
        this(DEFAULT_INIT_PARAM);
    }

    protected Settings createSettings(ServletContext context) {
        String[] settingsLocations = settingsLocations(context);
        SettingsBuilder sb = SettingsBuilder.createDefault();
        try {
            for (String loc : settingsLocations) {
                if (Streams.locate(loc) == null) {
                    throw new ConfigurationError("No such settings file: " + loc);
                }
                if (loc.indexOf(":/") > 0) {
                    URL url = new URL(loc);
                    sb.add(url);
                } else {
                    sb.add(loc);
                }
            }
            Properties props = new Properties();
            for (Enumeration<?> en = context.getInitParameterNames(); en.hasMoreElements();) {
                Object o = en.nextElement();
                if (o instanceof String) {
                    String key = (String) o;
                    String val = context.getInitParameter(key);
                    if (val != null) {
                        props.setProperty(key, val);
                    }
                }
            }
            if (!props.isEmpty()) {
                sb.add(props);
            }
            return sb.buildMutableSettings();
        } catch (IOException ex) {
            throw new ConfigurationError(ex);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected final Settings configure(ServletContext context, List<? super Module> addModulesHere) throws Exception {
        Settings settings = createSettings(context);
        final String modulesString = context.getInitParameter(moduleListInitParam);
        if (modulesString != null) {
            final String[] moduleStrings = modulesString.trim().split(",");
            for (final String moduleString : moduleStrings) {
                final Class<? extends Module> clazz = (Class<? extends Module>) Thread.currentThread().getContextClassLoader().loadClass(moduleString.trim());
                final Module module = instantiateModule(clazz, settings);
                addModulesHere.add(module);
            }

        }
        return settings;
    }

    /**
     * Provide locations in addition to the standard ones to load system
     * settings from.  These may be paths on the classpath, or URLs
     * 
     * The default implementation returns an empty array.
     * 
     * @param context The servlet context (
     * @return 
     */
    protected String[] settingsLocations(ServletContext context) {
        return new String[0];
    }

    private Module instantiateModule(Class<? extends Module> moduleClass, Settings settings) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException {
        Module module;
        Constructor c = findUsableModuleConstructor(moduleClass);
        c.setAccessible(true);
        if (c.getParameterTypes().length == 1) {
            module = (Module) c.newInstance(settings);
        } else if (c.getParameterTypes().length == 0) {
            module = (Module) c.newInstance();
        } else {
            throw new AssertionError("Cannot instantiate a module class "
                    + moduleClass.getName()
                    + " with constructor arguments which are not empty or a"
                    + " single Settings object");
        }
        return module;
    }

    static Constructor<?> findUsableModuleConstructor(Class<?> type) throws NoSuchMethodException {
        Constructor<?> con = null;
        try {
            con = type.getDeclaredConstructor(Settings.class);
        } catch (NoSuchMethodException ex) {
            con = type.getDeclaredConstructor();
        }
        return con;
    }
}
