package com.mastfrog.email.server;

import com.mastfrog.url.Host;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Test;

public class EmailAddressTest {
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
