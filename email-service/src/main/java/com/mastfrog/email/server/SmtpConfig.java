package com.mastfrog.email.server;

import com.google.inject.ImplementedBy;
import com.mastfrog.url.Host;

/**
 * Default SMTP server settings for actually sending mail.
 *
 * @author Tim Boudreau
 */
@ImplementedBy(DefaultSmtpConfig.class)
public interface SmtpConfig {
    //settings for the default Settings-based configuration
    public static final String SMTP_USERNAME_SETTINGS_KEY = "smtp.username";
    public static final String SMTP_PASSWORD_SETTINGS_KEY = "smtp.password";
    public static final String SMTP_HOST_SETTINGS_KEY = "smtp.host";
    public static final String SMTP_PORT_SETTINGS_KEY = "smtp.port";
    public static final String SMTP_USE_TLS_SETTINGS_KEY = "smtp.use.tls";
    public static final String SMTP_USE_SSL_SETTINGS_KEY = "smtp.use.ssl";

    Host getHost();

    String getPassword();

    int getPort();

    String getUsername();

    boolean isUseTLS();
    
    boolean isUseSSL();
}
