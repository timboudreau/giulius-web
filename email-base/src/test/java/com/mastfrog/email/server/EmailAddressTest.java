package com.mastfrog.email.server;

import com.mastfrog.email.EmailAddress;
import com.mastfrog.url.Host;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.netbeans.validation.api.Problems;

public class EmailAddressTest {

    @Test
    public void testNorm() {
        String s = "TIMOTHY <TIMOTHY.BOUDREAU@ORACLE.COM>";
        EmailAddress a = new EmailAddress(s);
        Problems p = a.getProblems();
        assertFalse(p.getLeadProblem() + "", p.hasFatal());
        assertTrue(a.isValid());
        assertEquals("TIMOTHY", a.getPersonalName());
        assertEquals("timothy.boudreau@oracle.com", a.getAddressPart());
    }

    @Test
    public void testQuotes() {
        EmailAddress a = new EmailAddress("\"Blog, Robert\" <RobertJ_Blog@QXMC.foo.edu>");
        assertEquals("Blog, Robert", a.getPersonalName());
        assertEquals("Robert Blog", a.normalize().getPersonalName());

        a = new EmailAddress("Robert Blog (Coog) <RobertJ_Blog@QXMC.foo.edu>");
        assertEquals("Robert Blog", a.normalize().getPersonalName());

        a = new EmailAddress("Robert Blog+stuff <RobertJ_Blog@QXMC.foo.edu>");
        assertEquals("Robert Blog", a.normalize().getPersonalName());

        a = new EmailAddress("Robert Blog+stuff <RobertJ_Blog+garbage@QXMC.foo.edu>");
        assertEquals("Robert Blog", a.normalize().getPersonalName());
        assertEquals("robertj_blog@qxmc.foo.edu", a.normalize().getAddressPart());
    }

    @Test
    public void test() {
        String x = "\"MongoLab Operations <support@mongolab.com>\" <support@mongolab.com>";
        EmailAddress addr = new EmailAddress(x);
        assertEquals("support@mongolab.com", addr.getAddressPart());
    }

    @Test
    public void testEquals() {
        testEquals("Tim Boudreau <tim@vy.gd>", "tim@vy.gd");
        testEquals("Tim Boudreau<tim@vy.gd>", "tim@vy.gd");
        testEquals("Tim <tim@vy.gd>", "tim@vy.gd");
        testEquals("Tim<tim@vy.gd>", "tim@vy.gd");
        testEquals("Tim<tim@vy.gd>", "tim@vy.gd ");
        testEquals("Tim Boudreau <tim@vy.gd >", "tim@vy.gd");
        testEquals(" Tim Boudreau <tim@vy.gd>", "tim@vy.gd");
        testEquals(" Joe Blow<tim@vy.gd>", "tim@vy.gd");
        testEquals("Joe Blow<tim@vy.gd>", "Waaa<tim@vy.gd>");
        testEquals(" Tim Boudreau <tim@vy.gd>", "tim@vy.gd ");
        testEquals("<tim@vy.gd>", "tim@vy.gd ");
        testEquals("<tim@vy.gd>", "tim@vy.gd");
        testEquals("tim@vy.gd", "tim@vy.gd");
        testEquals("Tim@vy.gd", "tim@vy.gd");
        testEquals("TiM@vy.gd", "tim@vy.gd");
        testEquals("TiM@fOoBaR.gd", "tim@foobar.gd");

        testNotEquals("<miko@vy.gd>", "tim@vy.gd");
        testNotEquals("miko@vy.gd", "tim@vy.gd");
        testNotEquals("tom@vy.gd", "tim@vy.gd");
        testNotEquals("tim@netbeans.com", "tim@vy.gd");
    }

    @Test
    public void testHost() {
        assertEquals (new EmailAddress("tim@vy.gd").getHost(), Host.parse("vy.gd"));
        assertEquals ("tim", new EmailAddress("tim@vy.gd").getPersonalName());
        assertEquals ("tim", new EmailAddress("Tim@vy.gd").getPersonalName());
        assertEquals (new EmailAddress("tim@vy.gd").getHost(), Host.parse("vY.gd"));
    }

    @Test
    public void testName() {
        assertEquals ("Tim Boudreau", new EmailAddress("Tim Boudreau <tim@vy.gd>").getPersonalName());
        assertEquals ("Tim Boudreau", new EmailAddress("  Tim Boudreau <tim@vy.gd>").getPersonalName());
    }

    private void testEquals (String a, String b) {
        assertEquals (new EmailAddress(a), new EmailAddress(b));
    }

    private void testNotEquals (String a, String b) {
        assertFalse (new EmailAddress(a).equals(new EmailAddress(b)));
    }
}
