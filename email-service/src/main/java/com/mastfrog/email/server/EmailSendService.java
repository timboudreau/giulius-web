package com.mastfrog.email.server;

import com.google.inject.ImplementedBy;
import java.util.Map;
import org.apache.commons.mail.Email;

/**
 * Service for sending email
 *
 * @author Tim Boudreau
 */
@ImplementedBy(EmailSendServiceImpl.class)
public interface EmailSendService {

    String BCC_LIST_SETTING = "smtp.bcc";

    <E extends Email> void send(PublishListener<E> l, String subject, String body, Map<String, Object> injected, EmailAddress from, String... to);
    <E extends Email, T extends Enum<T>> void send(T template, PublishListener<E> l, String subject, String body, Map<String, Object> injected, EmailAddress from, String... to);
}
