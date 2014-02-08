package com.mastfrog.email.server;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mastfrog.url.Host;
import com.mastfrog.util.Checks;


/**
 *
 * @author Tim Boudreau
 */
final class DefaultSmtpConfig implements SmtpConfig {
    
    @Inject(optional=true)
    @Named(SMTP_USERNAME_SETTINGS_KEY)
    private String username;
    
    @Inject(optional=true)
    @Named(SMTP_PASSWORD_SETTINGS_KEY)
    private String password;
    
    @Inject(optional=true)
    @Named(SMTP_USE_TLS_SETTINGS_KEY)
    private boolean useTls;
    
    @Inject(optional=true)
    @Named(SMTP_USE_SSL_SETTINGS_KEY)
    private boolean useSSL;
    private final Host host;
    private final int port;

    @Inject
    public DefaultSmtpConfig(@Named(SMTP_HOST_SETTINGS_KEY) String host, @Named(SMTP_PORT_SETTINGS_KEY) int port) {
        Checks.nonNegative("port", port);
        this.port = port;
        Host h = Host.parse(host);
        if (!h.isValid()) {
            throw new Error (h.getProblems().getLeadProblem().toString());
        }
        this.host = h;
    }

    @Override
    public Host getHost() {
        return host;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public boolean isUseTLS() {
        return useTls;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isUseSSL() {
        return useSSL;
    }
}
