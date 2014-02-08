package com.mastfrog.email.server;

import com.google.inject.ImplementedBy;
import java.util.Map;

/**
 * Service for sending email
 *
 * @author Tim Boudreau
 */
@ImplementedBy(EmailSendServiceImpl.class)
public interface EmailSendService {

    String BCC_LIST_SETTING = "smtp.bcc";

    void send(PublishListener l, String subject, String body, Map<String, Object> injected, EmailAddress from, String... to);
}
