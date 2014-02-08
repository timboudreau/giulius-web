package com.mastfrog.email.server;

import com.google.inject.Inject;
import com.mastfrog.util.Checks;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.openide.util.Exceptions;

/**
 * MessageService implementation which takes a Message and formats it into an
 * email.
 *
 * @author Tim Boudreau
 */
final class EmailSendServiceImpl implements EmailSendService {

    private final List<String> bccList = new ArrayList<String>();
    private final EmailServerService sender;
    private final HtmlMessageFormatter formatter;

    @Inject
    public EmailSendServiceImpl(EmailServerService sender, HtmlMessageFormatter formatter) {
        this.sender = sender;
        this.formatter = formatter;
    }

    private String generateHtmlBody(EmailAddress sender, String subject, String body, Map<String, Object> injected) {
        return formatter.format(sender, subject, body, injected);
    }

    public void send(PublishListener l, String subject, String body, Map<String, Object> injected, EmailAddress from, String... to) {
        Checks.notEmptyOrNull("to", to);
        for (String s : to) {
            new EmailAddress(s).getProblems().throwIfFatalPresent();
        }
        try {
            HtmlEmail email = new HtmlEmail();
            email.setSubject(subject);
            for (String a : to) {
                email.addTo(a);
            }
            for (String a : bccList) {
                if (a == null) {
                    System.err.println("NULL IN BCC LIST");
                }
                email.addBcc(a);
            }
            email.setReplyTo(Arrays.<InternetAddress>asList(InternetAddress.parse(from.toString())));
            if (from != null) {
                email.setFrom(from.toString());
            }
            String htmlBody = generateHtmlBody(from, subject, body, injected);
            if (htmlBody != null) {
                email.setHtmlMsg(htmlBody);
            }
            String plainBody = body;
            email.setTextMsg(plainBody);
            System.out.println("Sending message " + email);
            sender.send(email, l);
        } catch (EmailException | AddressException ex) {
            Logger.getLogger(EmailSendServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex); //for now
        }
    }
}
