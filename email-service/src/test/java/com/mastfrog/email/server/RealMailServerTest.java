package com.mastfrog.email.server;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.mastfrog.email.server.RealMailServerTest.M;
import com.mastfrog.giulius.tests.anno.Configurations;
import com.mastfrog.giulius.tests.GuiceRunner;
import com.mastfrog.giulius.tests.anno.TestWith;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Tim Boudreau
 */
@RunWith(GuiceRunner.class)
@TestWith({M.class})
public class RealMailServerTest {

    @Test
    public void testConfiguration(EmailServerService service) {
        assertNotNull(service);
        assertTrue(service instanceof StubMailServer);

        StubMailServer rms = (StubMailServer) service;
        assertNotNull(rms.addressConfig);

        assertNotNull(rms.getDefaultFromAddress());
        assertEquals("nobody@nowhere.com", rms.getDefaultFromAddress());

        assertNotNull(rms.addressConfig.getRecipientRedirectionAddress());
        assertEquals(new EmailAddress("somebody@somewhere.com"), rms.addressConfig.getRecipientRedirectionAddress());
    }

    @Test
    public void testSend(EmailServerService service) throws EmailException, QueueFullException {
        Email email = new SimpleEmail();
//        email.setHostName("smtp.gmail.com");
//        email.setSmtpPort(587);
//        email.setAuthenticator(new DefaultAuthenticator("username", "password"));
//        email.setTLS(true);
        email.setFrom("user@gmail.com");
        email.setSubject("TestMail");
        email.setMsg("This is a test mail ... :-)");
        email.addTo("foo@bar.com");
        final boolean[] success = new boolean[1];
        PublishListener<Email> pl = new PublishListener<Email>() {

            @Override
            public void progress(int i, int steps, String string, Email email) {
            }

            @Override
            public void onSuccess(Email email) {
                success[0] = true;
                synchronized (this) {
                    notifyAll();
                }
            }

            Throwable failure;

            @Override
            public synchronized void onFailure(Throwable failure, Email message) {
                this.failure = failure;
                failure.printStackTrace();
            }
        };
        service.send(email, pl);
    }

    @Test
    @Configurations("com/mastfrog/email/server/SmtpConfigProperties.properties")
    public void testSmtpConfig(DefaultSmtpConfig config) {
        assertTrue(config.isUseTLS());
        assertEquals("mail.host.test", config.getHost().toString());
        assertEquals("user", config.getUsername());
        assertEquals(123, config.getPort());
        assertEquals("password", config.getPassword());
    }

    @Test(expected = Error.class)
    @Configurations("com/mastfrog/email/server/InvalidSmtpConfigProperties.properties")
    public void testBadConfig(DefaultSmtpConfig config) {

    }

    @Test(expected = IllegalArgumentException.class)
    @Configurations("com/mastfrog/email/server/InvalidSmtpConfigProperties2.properties")
    public void testBadConfig2(DefaultSmtpConfig config) {

    }

    @Test(expected = Throwable.class)
    @Configurations("com/mastfrog/email/server/InvalidSmtpConfigProperties3.properties")
    public void testBadConfig3(DefaultSmtpConfig config) {

    }

    static class M extends AbstractModule {

        @Override
        protected void configure() {
            bind(EmailServerService.class).to(StubMailServer.class).in(Scopes.SINGLETON);
            bind(ExecutorService.class).annotatedWith(Names.named("mailqueue")).toInstance(Executors.newCachedThreadPool());
        }
    }
}
