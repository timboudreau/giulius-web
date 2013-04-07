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
package com.mastfrog.statistics;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matcher;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import com.mastfrog.giulius.ShutdownHookRegistry;
import com.mastfrog.guicy.annotations.Defaults;
import com.mastfrog.settings.RefreshInterval;
import com.mastfrog.settings.SettingsRefreshInterval;
import com.mastfrog.settings.Settings;
import java.lang.management.ManagementFactory;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Module which handles the &#064;Benchmark annotation by creating an MBean
 * which will collect statistics on each such case.
 *
 * @author Tim Boudreau
 */
@Defaults("stats.enable.udp=true")
public final class JmxAopModule extends AbstractModule {
    private MBeanServer mbeanServer;
    private UDPBroadcaster broadcaster;
    public static final String ENABLE_UDP = "stats.enable.udp";
    private boolean enableUdp;

    JmxAopModule(Settings settings) {
        enableUdp = settings.getBoolean(ENABLE_UDP, true);
    }

    @Override
    protected void configure() {
        mbeanServer = ManagementFactory.getPlatformMBeanServer();
        bind(MBeanServer.class).toInstance(mbeanServer);
        Matcher<AnnotatedElement> m = Matchers.annotatedWith(Benchmark.class);
        binder().bindInterceptor(Matchers.any(), m, new Benchmarker(mbeanServer));
        for (RefreshInterval r : SettingsRefreshInterval.values()) {
            try {
                IntervalsMBean bean = new Intervals(r);
                ObjectName name = new ObjectName(r.getClass().getName(), "type", r.name());
                mbeanServer.registerMBean(bean, name);
            } catch (MBeanRegistrationException | NotCompliantMBeanException | MalformedObjectNameException ex) {
                Logger.getLogger(JmxAopModule.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstanceAlreadyExistsException e) {
                //do nothing
            }
        }
        bindSettingsBean();
        if (enableUdp) {
            try {
                bind(UDPBroadcaster.class).toInstance(broadcaster = new UDPBroadcaster("224.0.0.1", 43124, ShutdownHookRegistry.get()));
            } catch (UnknownHostException ex) {
                Logger.getLogger(JmxAopModule.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SocketException ex) {
                Logger.getLogger(JmxAopModule.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void bindSettingsBean() {
        final boolean[] bound = new boolean[1];
        Matcher<? super TypeLiteral<?>> m = Matchers.any();//new SettingsMatcher();

        binder().bindListener(m, new TypeListener() {
            @Override
            public <I> void hear(TypeLiteral<I> tl, TypeEncounter<I> te) {
                if (!bound[0]) {
                    bound[0] = true;
                    registerSettingsBean(te);
                }
            }
        });
    }

    private void registerSettingsBean(TypeEncounter te) {
        System.err.println("register settings bean");
        //XXX handle namespaced settings
        try {
            ObjectName settingsName = new ObjectName(Settings.class.getPackage().getName(), "type", "Settings");
            Provider<Settings> provider = te.getProvider(Settings.class);
            SettingsBean bean = new SettingsBean(provider);
            mbeanServer.registerMBean(bean, settingsName);
        } catch (InstanceAlreadyExistsException ex) {
            Logger.getLogger(JmxAopModule.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MBeanRegistrationException | NotCompliantMBeanException | MalformedObjectNameException ex) {
            throw new Error(ex);
        }
    }
    private class Benchmarker implements MethodInterceptor {
        private final MBeanServer mbeanServer;
        private Map<String, Stats> beans = new HashMap<String, Stats>();

        private Benchmarker(MBeanServer mbeanServer) {
            this.mbeanServer = mbeanServer;
        }

        @Override
        public Object invoke(MethodInvocation mi) throws Throwable {
            try {
                Benchmark bmk = mi.getMethod().getAnnotation(Benchmark.class);
                String name = bmk.value();
                Stats bean = beans.get(name);
                if (bean == null) {
                    synchronized (this) {
                        bean = beans.get(name);
                        if (bean == null) {
                            bean = new Stats();
                            bean.name = name;
                            beans.put(name, bean);
                            ObjectName on = new ObjectName(mi.getThis().getClass().getPackage().getName(), "type", mi.getThis().getClass().getSuperclass().getSimpleName());
                            mbeanServer.registerMBean(bean, on);
                        }
                    }
                }
                return benchmark(bean, bmk, mi);
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }

        Object benchmark(Stats bean, Benchmark bmk, MethodInvocation inv) throws Throwable {
            bean.count.incrementAndGet();
            long start = System.currentTimeMillis();
            bean.contention.incrementAndGet();
            try {
                return inv.proceed();
            } catch (InvocationTargetException ex) {
                if (ex.getCause() != null) {
                    throw ex.getCause();
                }
                throw ex;
            } finally {
                bean.contention.decrementAndGet();
                long duration = System.currentTimeMillis() - start;
                bean.longestTime.setMaximum((int) duration);
                bean.total.addAndGet(duration);
                if (broadcaster != null) {
                    for (Benchmark.Kind kind : bmk.publish()) {
                        UDPMessage message;
                        switch (kind) {
                            case CALL_COUNT:
                                message = new UDPMessage(bmk.value(), kind, bean.getInvocationCount());
                                break;
                            case TOTAL_TIME:
                                message = new UDPMessage(bmk.value(), kind, bean.getTotalTimeSpent());
                                break;
                            default:
                                throw new AssertionError(kind);
                        }
                        broadcaster.publish(message);
                    }
                }
            }
        }
    }
}
