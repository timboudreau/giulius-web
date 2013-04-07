package com.mastfrog.statistics;

import com.google.inject.Inject;
import com.mastfrog.giulius.Dependencies;
import com.mastfrog.settings.RefreshInterval;
import com.mastfrog.settings.Settings;
import com.mastfrog.settings.SettingsBuilder;
import com.mastfrog.settings.SettingsRefreshInterval;
import com.mastfrog.statistics.Benchmark.Kind;
import java.io.IOException;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest {
    @Test
    public void testUDPMessage() {
        UDPMessage msg = new UDPMessage("hello", Kind.CALL_COUNT, 1, 2, 3);
        UDPMessage des = new UDPMessage(msg.toByteArray());
        assertEquals(msg, des);

        UDPMessage b = new UDPMessage("goodbye", Kind.TOTAL_TIME, 23);
        
        byte[] combined = (msg.toStringForm() + '|' + b.toStringForm() + '|' + des.toStringForm()).getBytes(UDPBroadcaster.ascii);
        UDPMessage[] msgs = UDPMessage.parse(combined);
        
        assertEquals (msg, msgs[0]);
        assertEquals (b, msgs[1]);
        assertEquals (des, msgs[2]);
        
    }

    @Test
    public void testSettingsBean() throws Exception {
        Dependencies deps = new Dependencies(SettingsBuilder.createDefault().build(), new JmxAopModule(SettingsBuilder.createDefault().build()));
        MBeanServer serv = deps.getInstance(MBeanServer.class);
        assertNotNull(serv);
        for (RefreshInterval r : SettingsRefreshInterval.values()) {
            ObjectName name = new ObjectName(r.getClass().getName(), "type", r.name());
            ObjectInstance in = serv.getObjectInstance(name);
            assertNotNull(in);
            Thread.sleep(100);
        }

        Settings s = deps.getInstance(Settings.class);
        X x = deps.getInstance(X.class);
        assertSame("Settings should be a singleton", s, x.settings);

        MBeanServer bs = deps.getInstance(MBeanServer.class);
        assertNotNull(bs);
        ObjectName settingsName = new ObjectName(Settings.class.getPackage().getName(), "type", "Settings");

        for (String ss : bs.getDomains()) {
            System.out.println(ss);
        }

        assertTrue(bs.isRegistered(settingsName));

        ObjectInstance o = bs.getObjectInstance(settingsName);
        assertNotNull(o);
        assertEquals(SettingsBean.class.getName(), o.getClassName());
        assertEquals(settingsName, o.getObjectName());
    }

    @Test
    public void testInterception() throws IOException, InterruptedException {
        Dependencies deps = new Dependencies(SettingsBuilder.createDefault().add(JmxAopModule.ENABLE_UDP, "true").build(), new JmxAopModule(SettingsBuilder.createDefault().build()));
        InterceptedThing thing = deps.getInstance(InterceptedThing.class);
        for (int i = 0; i < 100; i++) {
            thing.doStuff();
        }
    }
    static class InterceptedThing {
        @Benchmark(value = "hello", publish = {Benchmark.Kind.CALL_COUNT, Benchmark.Kind.TOTAL_TIME})
        public void doStuff() throws InterruptedException {
            Thread.sleep(20);
        }
    }
    static class X {
        @Inject
        Settings settings;
    }
}
