package com.mastfrog.email.server;

import com.google.inject.ImplementedBy;
import java.util.logging.Logger;
import org.apache.commons.mail.Email;

/**
 * Service which offloads sending email to a background thread.
 *
 * @author Tim Boudreau
 */
@ImplementedBy(StubMailServer.class)
public interface EmailServerService {
    static final String SETTINGS_KEY_SMTP_DEBUG = "smtp.debug";
    static final Logger LOGGER = Logger.getLogger(StubMailServer.class.getName());
    static final String DEFAULT_FROM_EMAIL_ADDRESS_SETTINGS_KEY = "fallback.email.from.address";
    static final String EMAIL_REDIRECT_SETTINGS_KEY = "redirect.ALL.outbound.emails.to";
    static final String EMAIL_BOUNCE_ADDRESS_SETTINGS_KEY = "redirect.ALL.outbound.emails.to";
    void send(Email email, PublishListener<Email> listener) throws QueueFullException;
    String getDefaultFromAddress();
}
