package com.mastfrog.email.server;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import static com.mastfrog.email.server.EmailServerService.DEFAULT_FROM_EMAIL_ADDRESS_SETTINGS_KEY;
import static com.mastfrog.email.server.SmtpConfig.SMTP_HOST_SETTINGS_KEY;
import static com.mastfrog.email.server.SmtpConfig.SMTP_PASSWORD_SETTINGS_KEY;
import static com.mastfrog.email.server.SmtpConfig.SMTP_PORT_SETTINGS_KEY;
import static com.mastfrog.email.server.SmtpConfig.SMTP_USERNAME_SETTINGS_KEY;
import static com.mastfrog.email.server.SmtpConfig.SMTP_USE_SSL_SETTINGS_KEY;
import static com.mastfrog.email.server.SmtpConfig.SMTP_USE_TLS_SETTINGS_KEY;
import com.mastfrog.giulius.Dependencies;
import com.mastfrog.giulius.DependenciesBuilder;
import com.mastfrog.giulius.ShutdownHookRegistry;
import com.mastfrog.giulius.annotations.Namespace;
import com.mastfrog.settings.Settings;
import com.mastfrog.settings.SettingsBuilder;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.mail.Email;

/**
 *
 * @author Tim Boudreau
 */
public class LiveTest extends AbstractModule implements PublishListener<Email> {

    public static void main(String[] args) throws IOException, InterruptedException {
        Settings s = new SettingsBuilder()
                .add(SMTP_PORT_SETTINGS_KEY, "587")
                .add(SMTP_USERNAME_SETTINGS_KEY, "robot@timboudreau.com")
                .add(SMTP_PASSWORD_SETTINGS_KEY, "CHANGE IT")
                .add(SMTP_HOST_SETTINGS_KEY, "smtp.gmail.com")
                .add(DEFAULT_FROM_EMAIL_ADDRESS_SETTINGS_KEY, "tim@timboudreau.org")
                .add(SMTP_USE_TLS_SETTINGS_KEY, "true")
                .add(SMTP_USE_SSL_SETTINGS_KEY, "true")
                .add(Dependencies.SYSTEM_PROP_PRODUCTION_MODE, "true")
                .build();
        LiveTest lt = new LiveTest();

        Dependencies deps = new DependenciesBuilder()
                .add(s, Namespace.DEFAULT)
                .add(lt)
                .build();
        EmailSendServiceImpl service = deps.getInstance(EmailSendServiceImpl.class);

        service.send(lt, "And again....", "Woo hoo, this is a test of something or other too.  Will it be formatted better?\n"
                + "Lets see how paragraphs do!  I bet they do fine!\n\n--Tim", new HashMap<String, Object>(), new EmailAddress("robot@timboudreau.com"),
                "niftiness@gmail.com");

        lt.await();
        deps.shutdown();
    }

    CountDownLatch latch = new CountDownLatch(1);

    void await() throws InterruptedException {
        latch.await();
    }

    @Override
    public void progress(int i, int steps, String string, Email email) {
    }

    @Override
    public void onSuccess(Email email) {
        latch.countDown();
    }

    @Override
    public void onFailure(Throwable failure, Email message) {
        failure.printStackTrace();
        latch.countDown();
    }

    @Override
    protected void configure() {
        bind(ExecutorService.class).annotatedWith(Names.named("mailqueue")).toInstance(Executors.newCachedThreadPool());
        bind(EmailServerService.class).to(RealMailServer.class);
        bind(ShutdownEmail.class).asEagerSingleton();
    }

    static class ShutdownEmail implements Runnable {

        private final ExecutorService svc;

        @Inject
        ShutdownEmail(@Named("mailqueue") ExecutorService svc, ShutdownHookRegistry reg) {
            this.svc = svc;
            reg.add(this);
        }

        @Override
        public void run() {
            svc.shutdown();
        }
    }

}
