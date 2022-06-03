package com.mastfrog.email.server;

import com.mastfrog.email.EmailAddress;
import com.mastfrog.giulius.tests.GuiceRunner;
import com.mastfrog.giulius.tests.TestWith;
import com.mastfrog.util.collections.MapBuilder;
import java.util.Map;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Tim Boudreau
 */
@RunWith(GuiceRunner.class)
@TestWith(EmailServiceModule.class)
public class TestFormatter {

    @Test
    public void test(HtmlMessageFormatter formatter) {
        Map<String, Object> model = new MapBuilder().put("subhead", 23578).build();
        String result = formatter.format(null, new EmailAddress("foo@bar.com"), "A unique idea & subject", "Woo hoo the message", model);
        assertTrue(result.contains("Woo hoo the message"));
        assertTrue(result.contains("A unique idea &amp; subject"));
        assertTrue(result.contains("23,578"));
    }
}
