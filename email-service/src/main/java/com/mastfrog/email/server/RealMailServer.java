package com.mastfrog.email.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mastfrog.giulius.ShutdownHookRegistry;
import com.mastfrog.settings.Settings;
import com.mastfrog.util.preconditions.ConfigurationError;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;

/**
 * Mail server which actually delivers mail to an SMTP server.
 *
 * @author Tim Boudreau
 */
@Singleton
final class RealMailServer extends StubMailServer {

    private final SmtpConfig smtpConfig;
    private boolean debug;

    @Inject
    public RealMailServer(SmtpConfig smtpConfig, EmailAddressesConfig config, @Named("mailqueue") ExecutorService threadPool, Settings settings, ShutdownHookRegistry vmShutdown) {
        super(config, threadPool, settings, vmShutdown);
        this.smtpConfig = smtpConfig;
        if ((smtpConfig.getUsername() == null) != (smtpConfig.getPassword() == null)) {
            throw new ConfigurationError("Both SMTP username and password must be configured, not just one - "
                    + " username=" + smtpConfig.getUsername() + " password " + smtpConfig.getPassword());
        }
        debug = settings.getBoolean(SETTINGS_KEY_SMTP_DEBUG, false);
    }

    @Override
    protected void prepareToSend(Email email) throws EmailException {
        super.prepareToSend(email);
        if (email.getFromAddress() == null) {
            email.setFrom(addressConfig.getDefaultSender().toString());
        }
    }

    @Override
    protected void reallySend(Email email) throws EmailException {
        email.setTLS(smtpConfig.isUseTLS());
        if (smtpConfig.getUsername() != null) {
            email.setAuthentication(smtpConfig.getUsername(), smtpConfig.getPassword());
        }
        email.setHostName(smtpConfig.getHost().toString());
        email.setSmtpPort(smtpConfig.getPort());
        email.setSSL(smtpConfig.isUseSSL());
        super.reallySend(email);
        email.setDebug(debug);
        email.send();
    }
}
